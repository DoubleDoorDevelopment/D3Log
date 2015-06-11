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

package net.doubledoordev.d3log.util;

import net.doubledoordev.d3log.D3Log;
import net.doubledoordev.d3log.logging.PlayerCache;
import net.doubledoordev.d3log.logging.TypeRegistry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * @author Dries007
 */
public class DBHelper
{
    private DBHelper()
    {
    }

    public static void setupDb()
    {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try
        {
            D3Log.getLogger().debug("Setup db");
            final String prefix = D3Log.getConfig().prefix;

            connection = D3Log.getDataSource().getConnection();
            statement = connection.createStatement();

            D3Log.getLogger().debug("Create table {}_types", prefix);
            String sql = "CREATE TABLE IF NOT EXISTS `" + prefix + "_types` (`type_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT, `type_name` VARCHAR(25) NOT NULL, PRIMARY KEY (`type_id`), UNIQUE KEY `type_name` (`type_name`) ) ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
            statement.execute(sql);

            D3Log.getLogger().debug("Create table {}_players", prefix);
            sql = "CREATE TABLE IF NOT EXISTS `" + prefix + "_players` (`player_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT, `player_name` VARCHAR(25) NOT NULL, `player_UUID` VARCHAR(36) NOT NULL, PRIMARY KEY (`player_id`), UNIQUE KEY `player_id` (`player_id`) ) ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
            statement.execute(sql);

            D3Log.getLogger().debug("Create table {}_data", prefix);
            sql = "CREATE TABLE IF NOT EXISTS `" + prefix + "_data` (`id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT, `epoch` INT(10) UNSIGNED NOT NULL, `type_id` INT(10) UNSIGNED NOT NULL, `player_id` INT(10) NULL, `dim` INT(10) NOT NULL, `x` INT(11) NOT NULL, `y` INT(11) NOT NULL, `z` INT(11) NOT NULL, `undone` BOOLEAN NOT NULL DEFAULT FALSE, PRIMARY KEY (`id`), KEY `epoch` (`epoch`), KEY `location` (`dim`, `x`, `z`, `y`, `type_id`)) ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
            statement.execute(sql);

            D3Log.getLogger().debug("Get table {}_extra_data", prefix);
            resultSet = connection.getMetaData().getTables(null, null, prefix + "_extra_data", null);
            if (!resultSet.next())
            {
                D3Log.getLogger().debug("Create table {}_extra_data", prefix);
                // extra data
                sql = "CREATE TABLE IF NOT EXISTS `" + prefix + "_extra_data` (`extra_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT, `data_id` INT(10) UNSIGNED NOT NULL, `data` TEXT NULL, PRIMARY KEY (`extra_id`), KEY `data_id` (`data_id`)) ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
                statement.executeUpdate(sql);

                D3Log.getLogger().debug("Create link between {}_data and {}_extra_data", prefix, prefix);
                // add extra data delete cascade
                sql = "ALTER TABLE `" + prefix + "_extra_data` ADD CONSTRAINT `" + prefix + "_extra_data_ibfk_1` FOREIGN KEY (`data_id`) REFERENCES `" + prefix + "_data` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;";
                statement.executeUpdate(sql);
            }

            D3Log.getLogger().debug("Get players from {}_players", prefix);
            resultSet = statement.executeQuery("SELECT `player_id`, `player_UUID` FROM " + prefix + "_players");
            while (resultSet.next())
            {
                int id = resultSet.getInt(1);
                UUID uuid = UUID.fromString(resultSet.getString(2));

                D3Log.getLogger().debug("Set player {} id to {}", uuid, id);

                PlayerCache.add(uuid, id);
            }
        }
        catch (final SQLException e)
        {
            D3Log.getLogger().error("DB issue", e);
        }
        finally
        {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }

        TypeRegistry.TYPE_REGISTRY.addFromDb();
    }

    public static void closeQuietly(AutoCloseable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (Exception ignored)
            {

            }
        }
    }
}
