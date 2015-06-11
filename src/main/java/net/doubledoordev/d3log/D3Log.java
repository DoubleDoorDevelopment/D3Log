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

package net.doubledoordev.d3log;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import net.doubledoordev.d3log.util.D3LogCommand;
import net.doubledoordev.d3log.util.D3LogConfig;
import net.doubledoordev.d3log.util.DBHelper;
import net.doubledoordev.d3log.util.libs.org.mcstats.Metrics;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.security.Policy;
import java.sql.SQLException;
import java.util.Map;

import static net.doubledoordev.d3log.logging.FMLEventHandler.FML_EVENT_HANDLER;
import static net.doubledoordev.d3log.logging.ForgeEventHandlers.FORGE_EVENT_HANDLERS;
import static net.doubledoordev.d3log.logging.LoggingThread.LOGGING_THREAD;
import static net.doubledoordev.d3log.lookups.WandHandler.WAND_HANDLER;
import static net.doubledoordev.d3log.util.Constants.MODID;

/**
 * @author Dries007
 */
@Mod(modid = MODID)
public class D3Log
{
    @Mod.Instance(MODID)
    public static D3Log instance;

    @Mod.Metadata(MODID)
    private static ModMetadata modMetadata;

    public D3LogConfig config;

    private File configDir;
    private Logger logger;
    private BasicDataSource dataSource;

    public static Logger getLogger()
    {
        return instance.logger;
    }

    public static BasicDataSource getDataSource()
    {
        return instance.dataSource;
    }

    public static D3LogConfig getConfig()
    {
        return instance.config;
    }

    @NetworkCheckHandler
    public boolean networkCheckHandler(Map<String, String> mods, Side side)
    {
        return true;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws IOException
    {
        if (event.getSide().isClient()) return;
        logger = event.getModLog();
        configDir = new File(event.getModConfigurationDirectory(), MODID);
        if (!configDir.exists()) configDir.mkdir();
        config = new D3LogConfig(configDir, event.getSuggestedConfigurationFile().getName());

        try
        {
            if (System.getProperty("java.security.policy") != null)
            {
                if (!config.configuration.getBoolean("ignoreJavaSecurityPolicy", MODID.toLowerCase(), false, "Don't touch this if you don't know what you are doing!"))
                {
                    config.save();
                    throw new SecurityException();
                }
            }
            else
            {
                File policyFile = new File(configDir, "d3Log.policy");
                FileUtils.writeStringToFile(policyFile, "grant {\n    permission javax.management.MBeanTrustPermission \"register\";\n};");
                System.setProperty("java.security.policy", policyFile.toURI().toURL().toString());
                Policy.getPolicy().refresh();
            }

            dataSource = BasicDataSourceFactory.createDataSource(config.dbProperties);
        }
        catch (SecurityException e)
        {
            RuntimeException e1 = new RuntimeException("\n\n\n" +
                    " __          __    _   ______ _____  _____   ____  _____     _    _____  ______          _____     _   __          __\n" +
                    " \\ \\        / /   | | |  ____|  __ \\|  __ \\ / __ \\|  __ \\   | |  |  __ \\|  ____|   /\\   |  __ \\   | |  \\ \\        / /\n" +
                    "  \\ \\      / /    | | | |__  | |__) | |__) | |  | | |__) |  | |  | |__) | |__     /  \\  | |  | |  | |   \\ \\      / / \n" +
                    "   \\ \\    / /     | | |  __| |  _  /|  _  /| |  | |  _  /   | |  |  _  /|  __|   / /\\ \\ | |  | |  | |    \\ \\    / /  \n" +
                    "    \\ \\  / /      |_| | |____| | \\ \\| | \\ \\| |__| | | \\ \\   |_|  | | \\ \\| |____ / ____ \\| |__| |  |_|     \\ \\  / /   \n" +
                    "     \\_\\/_/       (_) |______|_|  \\_\\_|  \\_\\\\____/|_|  \\_\\  (_)  |_|  \\_\\______/_/    \\_\\_____/   (_)      \\_\\/_/    \n\n\n" +
                    "We can't hack in a new security policy file or you have a custom one installed." +
                    "You need to add this line to the 'grant' part of it for D3Log to work:" +
                    "\n    permission javax.management.MBeanTrustPermission \"register\";\n" +
                    "You can disable this crash in the config.\n" +
                    "If you are using a server hosting company, ask them about it.", e);
            e1.setStackTrace(new StackTraceElement[0]);
            throw e1;
        }
        catch (Exception e)
        {
            RuntimeException e1 = new RuntimeException("\n\n\n" +
                    " __          __    _   ______ _____  _____   ____  _____     _    _____  ______          _____     _   __          __\n" +
                    " \\ \\        / /   | | |  ____|  __ \\|  __ \\ / __ \\|  __ \\   | |  |  __ \\|  ____|   /\\   |  __ \\   | |  \\ \\        / /\n" +
                    "  \\ \\      / /    | | | |__  | |__) | |__) | |  | | |__) |  | |  | |__) | |__     /  \\  | |  | |  | |   \\ \\      / / \n" +
                    "   \\ \\    / /     | | |  __| |  _  /|  _  /| |  | |  _  /   | |  |  _  /|  __|   / /\\ \\ | |  | |  | |    \\ \\    / /  \n" +
                    "    \\ \\  / /      |_| | |____| | \\ \\| | \\ \\| |__| | | \\ \\   |_|  | | \\ \\| |____ / ____ \\| |__| |  |_|     \\ \\  / /   \n" +
                    "     \\_\\/_/       (_) |______|_|  \\_\\_|  \\_\\\\____/|_|  \\_\\  (_)  |_|  \\_\\______/_/    \\_\\_____/   (_)      \\_\\/_/    \n\n\n" +
                    "There is a problem with your config file, did you enter the database information in the config??\n" +
                    "Is the MySQL server online??\n\n", e);
            e1.setStackTrace(new StackTraceElement[0]);
            throw e1;
        }

        MinecraftForge.EVENT_BUS.register(FORGE_EVENT_HANDLERS);
        MinecraftForge.EVENT_BUS.register(WAND_HANDLER);
        FMLCommonHandler.instance().bus().register(FML_EVENT_HANDLER);

        try
        {
            new Metrics(MODID, modMetadata.version).start();
        }
        catch (IOException e)
        {
            logger.error("Metrics error. ", e);
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        if (event.getSide().isClient()) return;

        event.registerServerCommand(new D3LogCommand());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event)
    {
        if (event.getSide().isClient()) return;
        DBHelper.setupDb();
        LOGGING_THREAD.start();
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event)
    {
        if (event.getSide().isClient()) return;
        LOGGING_THREAD.end();
        try
        {
            dataSource.close();
        }
        catch (SQLException e)
        {
            logger.error(e.getMessage(), e);
        }
    }
}
