/*
 * Copyright (c) 2014,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the {organization} nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package net.doubledoordev.d3log.logging;

import com.google.common.base.Strings;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.doubledoordev.d3log.D3Log;
import net.doubledoordev.d3log.logging.types.LogEvent;
import net.doubledoordev.d3log.util.Constants;
import net.doubledoordev.d3log.util.DBHelper;
import net.doubledoordev.d3log.util.UserProfile;
import net.minecraft.server.MinecraftServer;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Dries007
 */
public class LoggingThread extends Thread
{
    public static final LoggingThread LOGGING_THREAD = new LoggingThread();
    private boolean running = true;
    private int restartsDone = 0;
    private long lastRestart = System.currentTimeMillis();

    private LoggingThread()
    {
        super(Constants.MODID + "-LoggingThread");
    }

    @Override
    public void run()
    {
        while (running)
        {
            int doneEvents = 0;
            try
            {
                if (PlayerCache.TO_ADD_USER_PROFILES.size() != 0)
                {
                    doUUIDs();
                }
                if (LoggingQueue.getQueueSize() != 0)
                {
                    doneEvents = doBatch();
                }
                if (LoggingQueue.getQueueSize() == 0 || doneEvents == 0)
                {
                    D3Log.getLogger().debug("Waiting for {}s.", D3Log.getConfig().batchDelay);
                    waitFor(D3Log.getConfig().batchDelay);
                }
                else
                {
                    waitFor(1); // 1 sec minimum between logging batches to avoid console spam
                }
            }
            catch (Exception e)
            {
                D3Log.getLogger().error(e);
            }
        }
    }

    private void waitFor(int timeInSec)
    {
        try
        {
            synchronized (this)
            {
                this.wait(1000 * timeInSec);
            }
        }
        catch (InterruptedException ignored)
        {

        }
    }

    private void doUUIDs()
    {
        final String prefix = D3Log.getConfig().prefix;

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try
        {
            long start = System.currentTimeMillis();
            D3Log.getLogger().debug("Begin of batch UUID. Time: {} List size: {}", start, PlayerCache.TO_ADD_USER_PROFILES.size());

            connection = D3Log.getDataSource().getConnection();
            connection.setAutoCommit(false);

            statement = connection.prepareStatement("INSERT INTO " + prefix + "_players (`player_name`, `player_UUID`) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);

            ArrayList<UserProfile> profiles = new ArrayList<>(PlayerCache.TO_ADD_USER_PROFILES.size());
            while (PlayerCache.TO_ADD_USER_PROFILES.size() != 0)
            {
                UserProfile profile = PlayerCache.TO_ADD_USER_PROFILES.poll();
                profiles.add(profile);
                statement.setString(1, profile.getUsername());
                statement.setString(2, profile.getUuid().toString());
                statement.addBatch();
            }

            statement.executeBatch();
            connection.commit();

            resultSet = statement.getGeneratedKeys();
            for (UserProfile profile : profiles)
            {
                resultSet.next();
                profile.setId(resultSet.getInt(1));

                D3Log.getLogger().debug("Added player {} [{}] with id {} to DB", profile.getUsername(), profile.getUuid(), profile.getId());
            }

            long end = System.currentTimeMillis();
            D3Log.getLogger().debug("End of batch UUID in {} sec", (end - start) / 1000);
        }
        catch (SQLException e)
        {
            D3Log.getLogger().error("UUID insertion error. ", e);
        }
        finally
        {
            DBHelper.closeQuietly(connection);
            DBHelper.closeQuietly(statement);
            DBHelper.closeQuietly(resultSet);
        }
    }

    private int doBatch()
    {
        final String prefix = D3Log.getConfig().prefix;

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try
        {
            int maxPerBatch = D3Log.getConfig().maxPerBatch;
            int actionsRecorded = 0;
            long start = System.currentTimeMillis();
            D3Log.getLogger().debug("Begin of batch insert. Time: {} Queue size: {}", start, LoggingQueue.getQueueSize());

            ArrayList<LogEvent> extraDataList = new ArrayList<>(maxPerBatch);

            connection = D3Log.getDataSource().getConnection();
            connection.setAutoCommit(false);

            statement = connection.prepareStatement("INSERT INTO " + prefix + "_data (epoch,type_id,player_id,dim,x,y,z) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < maxPerBatch; i++)
            {
                final LogEvent event = LoggingQueue.getQueue().peek();
                if (event == null) break; // Done adding stuff

                UUID uuid = event.getUuid();
                if (uuid != null && !PlayerCache.hasIDFor(uuid))
                {
                    PlayerCache.TO_ADD_USER_PROFILES.add(new UserProfile(uuid));
                    break; // UUID not yet in database, commit this because then the profiles will be updated next round.
                }

                LoggingQueue.getQueue().poll(); // pick event off list

                extraDataList.add(event);

                statement.setLong(1, event.getEpoch());
                statement.setInt(2, event.getTypeId());
                if (uuid != null) statement.setInt(3, PlayerCache.getFromUUID(uuid).getId());
                else statement.setNull(3, Types.INTEGER);
                statement.setInt(4, event.getDim());
                statement.setInt(5, event.getX());
                statement.setInt(6, event.getY());
                statement.setInt(7, event.getZ());

                statement.addBatch();

                actionsRecorded++;
            }

            statement.executeBatch();
            connection.commit();

            resultSet = statement.getGeneratedKeys();

            statement = connection.prepareStatement("INSERT INTO " + prefix + "_extra_data (data_id,data) VALUES (?,?)");
            for (LogEvent event : extraDataList)
            {
                resultSet.next();
                String data = event.getData();
                if (Strings.isNullOrEmpty(data)) continue;

                statement.setInt(1, resultSet.getInt(1));
                statement.setString(2, data);
                statement.addBatch();
            }

            statement.executeBatch();
            connection.commit();

            long end = System.currentTimeMillis();
            D3Log.getLogger().debug("End of batch insert. Time: {} Queue size: {} Inserted {} events in {} sec", end, LoggingQueue.getQueueSize(), actionsRecorded, (end - start) / 1000);
            return actionsRecorded;
        }
        catch (SQLException eOriginal)
        {
            for (SQLException e = eOriginal; e != null; e = e.getNextException())
            {
                D3Log.getLogger().error("Batch insertion error.", e);
                D3Log.getLogger().error("SQL state: {}", e.getSQLState());
            }
        }
        finally
        {
            DBHelper.closeQuietly(connection);
            DBHelper.closeQuietly(statement);
            DBHelper.closeQuietly(resultSet);
        }
        return 0;
    }

    public void end()
    {
        running = false;
        this.interrupt();
    }

    @SubscribeEvent()
    public void serverTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START) return;

        if (!LOGGING_THREAD.isAlive())
        {
            D3Log.getLogger().fatal("#################################################################################");
            D3Log.getLogger().fatal("#################################################################################");
            D3Log.getLogger().fatal("#################################################################################");
            D3Log.getLogger().fatal("#################################################################################");
            D3Log.getLogger().fatal("####                                                                         ####");
            D3Log.getLogger().fatal("####                     The logger thread died!                             ####");
            D3Log.getLogger().fatal("####                                                                         ####");
            D3Log.getLogger().fatal("####          Depending on your config if will try to restart.               ####");
            D3Log.getLogger().fatal("####                                                                         ####");
            D3Log.getLogger().fatal("#################################################################################");
            D3Log.getLogger().fatal("#################################################################################");
            D3Log.getLogger().fatal("#################################################################################");
            D3Log.getLogger().fatal("#################################################################################");

            if (D3Log.getConfig().restartLogger)
            {
                if (System.currentTimeMillis() - lastRestart > 3600000) restartsDone = 0;

                if (D3Log.getConfig().restartAttempts != -1 && restartsDone >= D3Log.getConfig().restartAttempts)
                {
                    D3Log.getLogger().fatal("Restart limit reached. Shutdown");
                    MinecraftServer.getServer().initiateShutdown();
                }

                D3Log.getLogger().fatal("Trying a restart of the logger thread...");
                LOGGING_THREAD.start();
                restartsDone ++;
                lastRestart = System.currentTimeMillis();
            }
            else
            {
                D3Log.getLogger().fatal("Trying a restart of the logger thread...");
                MinecraftServer.getServer().initiateShutdown();
            }
        }
    }
}
