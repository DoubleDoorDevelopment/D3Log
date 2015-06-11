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
import com.mojang.authlib.GameProfile;
import net.doubledoordev.d3log.D3Log;
import net.doubledoordev.d3log.logging.types.LogEvent;
import net.doubledoordev.d3log.util.Constants;
import net.doubledoordev.d3log.util.DBHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Dries007
 */
public class LoggingThread extends Thread
{
    public static final LoggingThread LOGGING_THREAD = new LoggingThread();
    private final List<GameProfile> profiles = new ArrayList<>();
    private boolean running = true;

    private LoggingThread()
    {
        super(Constants.MODID + "-LoggingThread");
    }

    public static void login(GameProfile profile)
    {
        if (PlayerCache.hasUUID(profile.getId())) return;
        synchronized (LOGGING_THREAD.profiles)
        {
            LOGGING_THREAD.profiles.add(profile);
        }
    }

    @Override
    public void run()
    {
        while (running)
        {
            if (!profiles.isEmpty())
            {
                doUUIDs();
            }
            if (LoggingQueue.getQueueSize() != 0)
            {
                doBatch();
            }
            if (LoggingQueue.getQueueSize() == 0)
            {
                D3Log.getLogger().debug("Waiting for {}s.", D3Log.getConfig().batchDelay);
                try
                {
                    synchronized (this)
                    {
                        this.wait(1000 * D3Log.getConfig().batchDelay);
                    }
                }
                catch (InterruptedException ignored)
                {

                }
            }
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
            D3Log.getLogger().debug("Begin of batch UUID. Time: {} List size: {}", start, profiles.size());

            connection = D3Log.getDataSource().getConnection();
            connection.setAutoCommit(false);

            statement = connection.prepareStatement("INSERT INTO " + prefix + "_players (`player_name`, `player_UUID`) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
            synchronized (profiles)
            {
                for (GameProfile profile : profiles)
                {
                    statement.setString(1, profile.getName());
                    statement.setString(2, profile.getId().toString());
                    statement.addBatch();
                }

                statement.executeBatch();
                connection.commit();

                resultSet = statement.getGeneratedKeys();
                for (GameProfile profile : profiles)
                {
                    resultSet.next();
                    int id = resultSet.getInt(1);
                    PlayerCache.add(profile.getId(), id);

                    D3Log.getLogger().debug("Added player {} [{}] with id {} to DB", profile.getName(), profile.getId(), id);
                }
                profiles.clear();
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

    private void doBatch()
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
            for (int i = 0; i < maxPerBatch && LoggingQueue.getQueueSize() != 0; i++)
            {
                final LogEvent event = LoggingQueue.getQueue().peek();
                if (event == null) break;

                UUID uuid = event.getUuid();
                if (uuid != null && !PlayerCache.hasUUID(uuid)) break;

                LoggingQueue.getQueue().poll();

                extraDataList.add(event);

                statement.setLong(1, event.getEpoch());
                statement.setInt(2, event.getTypeId());
                if (uuid != null) statement.setInt(3, PlayerCache.getFromUUID(uuid));
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
            D3Log.getLogger().debug("End of batch insert. Time: {} Queue size: {}", end, LoggingQueue.getQueueSize());
            D3Log.getLogger().debug("Inserted {} events in {} sec", actionsRecorded, (end - start) / 1000);
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
    }

    public void end()
    {
        running = false;
        this.interrupt();
    }
}
