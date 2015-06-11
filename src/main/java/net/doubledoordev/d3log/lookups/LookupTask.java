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

package net.doubledoordev.d3log.lookups;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.doubledoordev.d3log.D3Log;
import net.doubledoordev.d3log.logging.PlayerCache;
import net.doubledoordev.d3log.logging.TypeRegistry;
import net.doubledoordev.d3log.logging.types.LogEvent;
import net.doubledoordev.d3log.util.DBHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import static net.doubledoordev.d3log.util.Constants.JOINER_AND;

/**
 * @author Dries007
 */
public class LookupTask implements Runnable
{
    private EntityPlayer owner;
    private boolean locationSet = false;
    private boolean rollback = false;
    private int x, y, z;
    private UUID uuid;
    private List<LogEvent> events;
    private boolean done = false;

    public LookupTask(EntityPlayer owner)
    {
        this.owner = owner;
    }

    public LookupTask setUUID(UUID uuid)
    {
        this.uuid = uuid;
        return this;
    }

    public LookupTask setLocation(int x, int y, int z)
    {
        locationSet = true;
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public LookupTask setRollback(boolean rollback)
    {
        this.rollback = rollback;
        return this;
    }

    public LookupTask go()
    {
        new Thread(this, "LookupTask-" + owner.getCommandSenderName() + "-" + x + "-" + y + "-" + z + "-" + uuid).start();
        if (rollback) FMLCommonHandler.instance().bus().register(this);
        return this;
    }

    @SubscribeEvent
    public void tickEvent(TickEvent.ServerTickEvent tick)
    {
        if (tick.phase == TickEvent.Phase.END || events == null || !done) return;

        owner.addChatComponentMessage(new ChatComponentText("Rollback queue: " + events.size()));

        Connection connection = null;
        PreparedStatement statement = null;
        try
        {
            final String prefix = D3Log.getConfig().prefix;

            connection = D3Log.getDataSource().getConnection();
            connection.setAutoCommit(false);
            statement = connection.prepareStatement("UPDATE " + prefix + "_data SET `undone` = '1' WHERE `id` = ?");

            int counter = 0;
            ListIterator<LogEvent> i = events.listIterator();
            while (i.hasNext() && counter < 1000)
            {
                LogEvent event = i.next();
                if (event.getType().canUndo)
                {
                    counter ++;
                    event.rollback();

                    statement.setInt(1, event.getID());
                    statement.addBatch();
                }
                i.remove();
            }

            statement.executeBatch();
            connection.commit();
        }
        catch (SQLException e)
        {
            D3Log.getLogger().error("Lookup insertion error. {} {} {} {}", x, y, z, owner);
            D3Log.getLogger().error("Exception:", e);
        }
        finally
        {
            DBHelper.closeQuietly(connection);
            DBHelper.closeQuietly(statement);
        }

        if (events.isEmpty()) FMLCommonHandler.instance().bus().unregister(this);
    }

    @Override
    public void run()
    {
        final String prefix = D3Log.getConfig().prefix;
        final long start = System.currentTimeMillis();

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try
        {
            D3Log.getLogger().debug("Begin of LookupTask Time: {}", start);

            connection = D3Log.getDataSource().getConnection();
            connection.setAutoCommit(false);

            statement = connection.createStatement();

            StringBuilder sql = new StringBuilder("SELECT * FROM `" + prefix + "_data` WHERE `undone` = FALSE AND ");
            List<String> parts = new ArrayList<>();
            if (locationSet) parts.add("`x` = " + x + " AND `y` = " + y + " AND `z` = " + z + " AND `dim` = " + owner.dimension);
            if (uuid != null) parts.add("`player_id` = " + PlayerCache.getFromUUID(uuid));
            JOINER_AND.appendTo(sql, parts);
            sql.append(" ORDER BY `id` DESC");

            statement.execute(sql.toString());
            resultSet = statement.getResultSet();

            events = new ArrayList<>(resultSet.getFetchSize());

            while (resultSet.next())
            {
                TypeRegistry.EventType eventType = TypeRegistry.TYPE_REGISTRY.get(resultSet.getInt(3));

                if (rollback && !eventType.canUndo) continue; // When we do a rollback, we don't need information on events that can't be rolled back.

                LogEvent logEvent = eventType.getNewInstance();
                logEvent.setType(eventType);
                logEvent.setID(resultSet.getInt(1));
                logEvent.setPosition(resultSet.getInt(5), resultSet.getInt(6), resultSet.getInt(7), resultSet.getInt(8));
                logEvent.setEpoch(resultSet.getInt(2));
                if (resultSet.getObject(4) != null)
                {
                    logEvent.setUuid(PlayerCache.getFromIn(resultSet.getInt(4)));
                }
                events.add(logEvent);
            }

            for (LogEvent event : events)
            {
                statement.execute("SELECT * FROM `" + prefix + "_extra_data` WHERE `data_id` = " + event.getID());
                resultSet = statement.getResultSet();

                if (resultSet.next())
                {
                    event.setData(resultSet.getString(3));
                    event.load();
                }

                if (!rollback)
                {
                    //TODO: Send ingame representation to client
                }
            }

            owner.addChatComponentMessage(new ChatComponentText("Events : " + events.size()));
            done = true;
//            {
//
//                StringBuilder msg = new StringBuilder();
//
//                msg.append("ID: ").append(resultSet.getInt(1)).append(' ');
//
//                if (!locationSet)
//                {
//                    msg.append(resultSet.getInt(6)).append(' ').append(resultSet.getInt(7)).append(' ').append(resultSet.getInt(8)).append(' ');
//                    S23PacketBlockChange packet = new S23PacketBlockChange(resultSet.getInt(6), resultSet.getInt(7), resultSet.getInt(8), owner.worldObj);
//
//                    packet.field_148883_d = Blocks.hay_block;
//                    packet.field_148884_e = 0;
//
//                    ((EntityPlayerMP) owner).playerNetServerHandler.sendPacket(packet);
//                }
//
//                int tDiff = time - resultSet.getInt(2);
//                if (tDiff / 86400 > 0) msg.append(tDiff / 86400).append("d ");
//                tDiff %= 86400;
//                if (tDiff / 3600 > 0) msg.append(tDiff / 3600).append("h ");
//                tDiff %= 3600;
//                if (tDiff / 60 > 0) msg.append(tDiff / 60).append("m ");
//                tDiff %= 60;
//                if (tDiff > 0) msg.append(tDiff).append("s ");
//
//                msg.append(TypeRegistry.TYPE_REGISTRY.get(resultSet.getInt(3)).name).append(' ');
//
//                if (uuid == null) msg.append("By: ").append(MinecraftServer.getServer().func_152358_ax().func_152652_a(PlayerCache.getFromIn(resultSet.getInt(4))).getName());
//
//                owner.addChatComponentMessage(new ChatComponentText(msg.toString()));
//            }
            D3Log.getLogger().debug("End of batch LookupTask {} ms", System.currentTimeMillis() - start);
        }
        catch (SQLException e)
        {
            D3Log.getLogger().error("Lookup insertion error. {} {} {} {}", x, y, z, owner);
            D3Log.getLogger().error("Exception:", e);
        }
        finally
        {
            DBHelper.closeQuietly(connection);
            DBHelper.closeQuietly(statement);
            DBHelper.closeQuietly(resultSet);
        }
    }
}
