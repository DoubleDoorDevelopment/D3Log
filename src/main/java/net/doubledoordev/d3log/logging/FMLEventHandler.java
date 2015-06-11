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

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.doubledoordev.d3log.logging.types.ItemsLogEvent;
import net.doubledoordev.d3log.logging.types.LogEvent;

import static net.doubledoordev.d3log.util.Constants.*;

/**
 * @author Dries007
 */
public class FMLEventHandler
{
    public static final FMLEventHandler FML_EVENT_HANDLER = new FMLEventHandler();

    private FMLEventHandler()
    {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerEvent_ChangeDim(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        LogEvent log = new LogEvent();

        log.setType(TYPE_CHANGE_DIM);
        log.setPlayerPosAndUuid(event.player);
        log.setData(String.valueOf(event.fromDim));

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerEvent_Login(PlayerEvent.PlayerLoggedInEvent event)
    {
        LoggingThread.login(event.player.getGameProfile());

        LogEvent log = new LogEvent();

        log.setType(TYPE_LOGIN);
        log.setPlayerPosAndUuid(event.player);

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerEvent_Logout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        LogEvent log = new LogEvent();

        log.setType(TYPE_LOGOUT);
        log.setPlayerPosAndUuid(event.player);

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerEvent_Respawn(PlayerEvent.PlayerRespawnEvent event)
    {
        LogEvent log = new LogEvent();

        log.setType(TYPE_RESPAWN);
        log.setPlayerPosAndUuid(event.player);

        LoggingQueue.addToQueue(log);
    }

    // BUGGED Forge event is used instead.
    //    @SubscribeEvent(priority = EventPriority.LOWEST)
    //    public void playerEvent_ItemPickup(PlayerEvent.ItemPickupEvent event)
    //    {
    //        ItemLogEvent log = new ItemLogEvent();
    //
    //        log.setType(TYPE_ITEM_PICKUP);
    //        log.setPlayerPosAndUuid(event.player);
    //        log.setData(event.pickedUp.getEntityItem());
    //
    //        LoggingQueue.addToQueue(log);
    //    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerEvent_ItemCrafting(PlayerEvent.ItemCraftedEvent event)
    {
        ItemsLogEvent log = new ItemsLogEvent();

        log.setType(TYPE_ITEM_CRAFTING);
        log.setPlayerPosAndUuid(event.player);
        log.setData(event.crafting);

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerEvent_ItemSmelting(PlayerEvent.ItemSmeltedEvent event)
    {
        ItemsLogEvent log = new ItemsLogEvent();

        log.setType(TYPE_ITEM_SMELTING);
        log.setPlayerPosAndUuid(event.player);
        log.setData(event.smelting);

        LoggingQueue.addToQueue(log);
    }
}
