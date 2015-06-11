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

import net.doubledoordev.d3log.lookups.LookupTask;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Dries007
 */
public class D3LogCommand extends CommandBase
{
    private static final String[] SUBCOMMANDS = new String[]{"help", "wand", "lookup", "rollback"};

    @Override
    public String getCommandName()
    {
        return "d3log";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_)
    {
        return "/d3log help";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (args.length == 0 || args[0].equalsIgnoreCase(SUBCOMMANDS[0]))
        {
            sender.addChatMessage(new ChatComponentText("--== 3DLog Help ==--").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.AQUA)));
            sender.addChatMessage(new ChatComponentText("Possible sub commands:"));
            sender.addChatMessage(new ChatComponentText("help -> Display this text"));
            sender.addChatMessage(new ChatComponentText("wand [target] -> Gives wand item to target or you."));
            sender.addChatMessage(new ChatComponentText("lookup <target> -> Look at the event history of target"));
            sender.addChatMessage(new ChatComponentText("rollback <target> -> Roll back all logged world changes by target."));
            sender.addChatMessage(new ChatComponentText("purge <time|all> -> Remove records older than time (or all) from the database."));
        }
        else if (args[0].equalsIgnoreCase(SUBCOMMANDS[1]))
        {
            EntityPlayerMP target;
            if (args.length == 2) target = getPlayer(sender, args[1]);
            else target = getCommandSenderAsPlayer(sender);

            target.inventory.addItemStackToInventory(Constants.WAND.copy());
        }
        else if (args[0].equalsIgnoreCase(SUBCOMMANDS[2]))
        {
            lookupTask(sender, args, false);
        }
        else if (args[0].equalsIgnoreCase(SUBCOMMANDS[3]))
        {
            lookupTask(sender, args, true);
        }
    }

    private void lookupTask(ICommandSender sender, String[] args, boolean rollback)
    {
        if (args.length != 2) throw new WrongUsageException("You must specify which player you wish to perform this action on.");
        UUID uuid;
        try
        {
            uuid = getPlayer(sender, args[1]).getUniqueID();
        }
        catch (PlayerNotFoundException ignored)
        {
            uuid = UUID.fromString(args[1]);
        }
        new LookupTask(getCommandSenderAsPlayer(sender)).setUUID(uuid).setRollback(rollback).go();
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);

        if (args.length == 2)
        {
            if (args[0].equalsIgnoreCase(SUBCOMMANDS[1])) return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
            if (args[0].equalsIgnoreCase(SUBCOMMANDS[2]) || args[0].equalsIgnoreCase(SUBCOMMANDS[3]))
            {
                List<String> list = new ArrayList<>();
                Collections.addAll(list, MinecraftServer.getServer().getAllUsernames());
                Collections.addAll(list, MinecraftServer.getServer().getConfigurationManager().getAvailablePlayerDat());
                return getListOfStringsFromIterableMatchingLastWord(args, list);
            }
        }
        return null;
    }
}
