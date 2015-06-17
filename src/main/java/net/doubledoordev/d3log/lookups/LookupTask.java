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

import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.doubledoordev.d3log.D3Log;
import net.doubledoordev.d3log.logging.PlayerCache;
import net.doubledoordev.d3log.logging.TypeRegistry;
import net.doubledoordev.d3log.logging.types.LogEvent;
import net.doubledoordev.d3log.util.DBHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.Sys;

import java.sql.*;
import java.util.*;

import static net.doubledoordev.d3log.util.Constants.JOINER_AND;

/**
 * @author Dries007
 */
public class LookupTask implements Runnable
{
    private static final Timer TIMER = new Timer();

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
            sql.append(" ORDER BY `id` ASC");

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

            final long time = (System.currentTimeMillis() / 1000);

            for (final LogEvent event : events)
            {
                statement.execute("SELECT * FROM `" + prefix + "_extra_data` WHERE `data_id` = " + event.getID());
                resultSet = statement.getResultSet();

                if (resultSet.next())
                {
                    event.setData(resultSet.getString(3));
                    event.load();
                }

                //TODO: Send ingame representation to client
                if (!rollback)
                {
                    ChatComponentText msg = new ChatComponentText(event.getID() + " ");
                    //StringBuilder msg = new StringBuilder();

                    //msg.append(event.getID()).append(" - ");

                    if (!locationSet)
                    {
                        msg.appendSibling(new ChatComponentText(event.getX() + ";" + event.getY() + ";" + event.getZ()).setChatStyle(new ChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Teleport to X:" + event.getX() + " Y:" + event.getY() + " Z:" + event.getZ()))).setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + event.getX() + " " + event.getY() + " " + event.getZ())).setColor(EnumChatFormatting.AQUA)));
                        //msg.append(event.getX()).append(';').append(event.getY()).append(';').append(event.getZ()).append(" - ");
                        S23PacketBlockChange packet = new S23PacketBlockChange(event.getX(), event.getY(), event.getZ(), owner.worldObj);

                        packet.field_148883_d = Blocks.hay_block;
                        packet.field_148884_e = 0;

                        ((EntityPlayerMP) owner).playerNetServerHandler.sendPacket(packet);

                        TIMER.schedule(new TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    ((EntityPlayerMP) owner).playerNetServerHandler.sendPacket(new S23PacketBlockChange(event.getX(), event.getY(), event.getZ(), owner.worldObj));
                                }
                                catch (Exception ignored)
                                {
                                    // Just to be safe.
                                }
                            }
                        }, 15 * 1000);
                    }

                    StringBuilder timePart = new StringBuilder(" [");
                    int tDiff = (int) (time - event.getEpoch());
                    if (tDiff / 86400 > 0) timePart.append(tDiff / 86400).append("d ");
                    tDiff %= 86400;
                    if (tDiff / 3600 > 0) timePart.append(tDiff / 3600).append("h ");
                    tDiff %= 3600;
                    if (tDiff / 60 > 0) timePart.append(tDiff / 60).append("m ");
                    tDiff %= 60;
                    if (tDiff > 0) timePart.append(tDiff).append("s] ");
                    msg.appendSibling(new ChatComponentText(timePart.toString()).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD)));

                    msg.appendSibling(new ChatComponentText(event.getType().name + " "));
                    //msg.append("- ").append(event.getType().name);

                    if (uuid == null && event.getUuid() != null)
                    {
                        String name = event.getUuid().toString();
                        GameProfile profile = MinecraftServer.getServer().func_152358_ax().func_152652_a(event.getUuid());
                        if (profile != null) name = profile.getName();
                        msg.appendSibling(new ChatComponentText(name + " ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GOLD)));
                    }
                    if (event.getType().hasHumanReadableString) msg.appendSibling(new ChatComponentText("\"" + event.getData() + "\"").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE)));

                    owner.addChatComponentMessage(msg);
                }
            }

            owner.addChatComponentMessage(new ChatComponentText("Lookup Events : " + events.size()));
            done = true;

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
