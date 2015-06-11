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

import net.doubledoordev.d3log.D3Log;
import net.doubledoordev.d3log.logging.types.*;
import net.doubledoordev.d3log.util.DBHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static net.doubledoordev.d3log.util.Constants.*;

/**
 * @author Dries007
 */
public class TypeRegistry
{
    public static final TypeRegistry TYPE_REGISTRY = new TypeRegistry();

    private boolean initialized;
    private final Map<String, EventType<?>> name_map = new HashMap<>();
    private final Map<Integer, EventType<?>> id_map = new HashMap<>();

    private TypeRegistry()
    {
        registerDefault();
    }

    public void register(String type, Class<? extends LogEvent> logEventClass, boolean canUndo)
    {
        if (name_map.containsKey(type)) throw new IllegalArgumentException("Duplicate key");
        name_map.put(type, new EventType<>(type, logEventClass, canUndo));
    }

    private void registerDefault()
    {
        register(TYPE_LOGIN, LogEvent.class, false);
        register(TYPE_LOGOUT, LogEvent.class, false);
        register(TYPE_RESPAWN, LogEvent.class, false);
        register(TYPE_CHANGE_DIM, LogEvent.class, false);
        register(TYPE_ITEM_PICKUP, ItemsLogEvent.class, false);
        register(TYPE_ITEM_CRAFTING, ItemsLogEvent.class, false);
        register(TYPE_ITEM_SMELTING, ItemsLogEvent.class, false);
        register(TYPE_ITEM_TOSS, ItemsLogEvent.class, false);
        register(TYPE_DAMAGE_GOT, DamageLogEvent.class, false);
        register(TYPE_DAMAGE_DEALT, DamageLogEvent.class, false);
        register(TYPE_KILLED, DamageLogEvent.class, false);
        register(TYPE_DIED, DamageLogEvent.class, false);
        register(TYPE_DROPS, ItemsLogEvent.class, false);
//        register(TYPE_ACHIEVEMENT, LogEvent.class, false);
        register(TYPE_ANVIL_REPAIR, ItemsLogEvent.class, false);
        register(TYPE_BONEMEAL, LogEvent.class, false);
        register(TYPE_INTERACT_ENTITY, EntityInteractLogEvent.class, false);
        register(TYPE_INTERACT_WORLD, WorldInteractLogEvent.class, false);
        register(TYPE_FILL_BUCKET, FillBucketLogEvent.class, true);
        register(TYPE_SLEEP, LogEvent.class, false);
        register(TYPE_BLOCK_BREAK, BlockSnapshotLogEvent.class, true);
        register(TYPE_BLOCK_PLACE, BlockPlaceLogEvent.class, true);
        register(TYPE_EXPLOSION_SOURCE, ExplosionSourceLogEvent.class, false);
        register(TYPE_EXPLOSION_DAMAGE, ExplosionDamageLogEvent.class, true);
        register(TYPE_COMMAND, LogEvent.class, false);
        register(TYPE_CHAT, LogEvent.class, false);
    }

    public void addFromDb()
    {
        if (initialized) return;
        initialized = true;

        final String prefix = D3Log.getConfig().prefix;

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try
        {
            connection = D3Log.getDataSource().getConnection();
            statement = connection.createStatement();

            D3Log.getLogger().debug("Get types from {}_types", prefix);
            resultSet = statement.executeQuery("SELECT `type_id`, `type_name` FROM " + prefix + "_types");

            while (resultSet.next())
            {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);

                EventType type = name_map.get(name);
                if (type != null)
                {
                    type.id = id;
                    D3Log.getLogger().debug("Set type {} id to {}", name, id);
                }
                else
                {
                    D3Log.getLogger().info("Type was in DB but not in game: {} with id {}", name, id);
                }
            }

            for (EventType type : name_map.values())
            {
                if (type.id == -1)
                {
                    statement.executeUpdate("INSERT INTO " + prefix + "_types (type_name) VALUES ('" + type.name + "')", Statement.RETURN_GENERATED_KEYS);
                    resultSet = statement.getGeneratedKeys();
                    resultSet.next();
                    type.id = resultSet.getInt(1);
                    D3Log.getLogger().debug("Added type {} with id {} to DB", type.name, type.id);
                }

                id_map.put(type.id, type);
            }
        }
        catch (final SQLException e)
        {
            D3Log.getLogger().error("DB issue", e);
        }
        finally
        {
            DBHelper.closeQuietly(resultSet);
            DBHelper.closeQuietly(statement);
            DBHelper.closeQuietly(connection);
        }
    }

    public EventType<? extends LogEvent> get(String type)
    {
        return name_map.get(type);
    }

    public EventType<? extends LogEvent> get(int id)
    {
        return id_map.get(id);
    }

    public static final class EventType<T extends LogEvent>
    {
        public final String name;
        public final boolean canUndo;

        private final Class<T> clazz;
        private int id = -1;

        public EventType(String name, Class<T> clazz, boolean canUndo)
        {
            this.name = name;
            this.clazz = clazz;
            this.canUndo = canUndo;
            getNewInstance(); // Just to make sure the this works later on.
        }

        public T getNewInstance()
        {
            try
            {
                T instance = clazz.newInstance();
                instance.setType(this);
                return instance;
            }
            catch (InstantiationException | IllegalAccessException e)
            {
                D3Log.getLogger().fatal("Probably no black constructor on this class: {}", clazz.getName());
                throw new RuntimeException(e);
            }
        }

        public int getId()
        {
            return id;
        }
    }
}
