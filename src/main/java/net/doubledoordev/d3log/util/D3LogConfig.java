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

import net.minecraftforge.common.config.Configuration;

import java.io.*;
import java.util.Properties;

import static net.doubledoordev.d3log.util.Constants.MODID;

/**
 * @author Dries007
 */
public class D3LogConfig
{
    public final Properties dbProperties = new Properties();
    public final int batchDelay;
    public final String prefix;
    public final int maxPerBatch;
    public final String username;
    public final String password;
    public final String dbName;
    public final int port;
    public final String host;
    public final Configuration configuration;
    public final String logLevel;
    public final int restartAttempts;
    public boolean restartLogger;

    public D3LogConfig(File configDir, String name) throws IOException
    {
        configuration = new Configuration(new File(configDir, name));
        configuration.addCustomCategoryComment(MODID, "There are some of the basic database properties.\n" +
                "For the more advanced stuff, open the database.properties file in this directory.\n" +
                "Anything with an asterisk (*) in the comment in this file can be overwritten in the database.properties!!");

        prefix = configuration.getString("prefix", MODID, MODID, "Prefix for the database tables.");
        batchDelay = configuration.getInt("batchDelay", MODID, 10, 1, 60, "Delay in between insertion batches.");
        maxPerBatch = configuration.getInt("maxPerBatch", MODID, 5000, 10, 1000000, "Maximum amount of events pushed to the DB per batch");
        username = configuration.getString("username", MODID, "root", "Your database username. Please don't use root... *");
        password = configuration.getString("password", MODID, "", "Your database password *");
        dbName = configuration.getString("dbName", MODID, "minecraft", "The database name *");
        port = configuration.getInt("port", MODID, 3306, 0, Short.MAX_VALUE, "The database port *");
        host = configuration.getString("host", MODID, "localhost", "The database host *");
        logLevel = configuration.getString("logLevel", MODID, "", "Because Log4j2 doesn't seem to like respecting external configuration files, I made this option. If blank, nothing is changed.");
        restartLogger = configuration.getBoolean("restartLogger", MODID, false, "If you set this to false, the server will stop when the logger fails.");
        restartAttempts = configuration.getInt("restartAttempts", MODID, 10, -1, Integer.MAX_VALUE, "Amount of logger thread failiours is accepted before server shutdown. -1 means infinite. Resets after 1 hour of no issues.");

        save();

        File propertiesFile = new File(configDir, "database.properties");

        if (propertiesFile.exists())
        {
            FileReader reader = new FileReader(propertiesFile);
            dbProperties.load(reader);
            reader.close();
        }
        else
        {
            //dbProperties.put("username", "minecraft");
            //dbProperties.put("password", "minecraft");
            dbProperties.put("driverClassName", "com.mysql.jdbc.Driver");
            //dbProperties.put("url", "jdbc:mysql://localhost:3306/minecraft");
            dbProperties.put("initialSize", "10");
            dbProperties.put("maxTotal", "20");
            dbProperties.put("maxIdle", "10");
            dbProperties.put("maxWaitMillis", "30000");
            dbProperties.put("removeAbandonedTimeout", "60");
            dbProperties.put("testOnBorrow", "true");
            dbProperties.put("validationQuery", "SELECT 1");
            dbProperties.put("validationQueryTimeout", "30000");
            //dbProperties.put("prefix", "d3Log");

            FileWriter fileWriter = new FileWriter(propertiesFile);
            dbProperties.store(fileWriter, "The database settings of D3Log\nSome things defined in this file will override the minecraft config file!!\nFor more information on how to configure this: http://commons.apache.org/proper/commons-dbcp/configuration.html");
            fileWriter.close();
        }

        if (!dbProperties.containsKey("username")) dbProperties.put("username", username);
        if (!dbProperties.containsKey("password")) dbProperties.put("password", password);
        if (!dbProperties.containsKey("url")) dbProperties.put("url", "jdbc:mysql://" + host + ":" + port + "/" + dbName);
    }

    public void save()
    {
        if (configuration.hasChanged()) configuration.save();
    }
}
