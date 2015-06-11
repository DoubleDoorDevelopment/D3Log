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

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.doubledoordev.d3log.D3Log;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import static net.doubledoordev.d3log.util.Constants.WAND;

/**
 * @author Dries007
 */
public class WandHandler
{
    public static final WandHandler WAND_HANDLER = new WandHandler();

    private WandHandler()
    {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void playerInteractEvent(PlayerInteractEvent event)
    {
        ItemStack stack = event.entityPlayer.getHeldItem();
        if (stack == null) return;

        if (WAND.getItem() == stack.getItem() && ItemStack.areItemStackTagsEqual(stack, WAND))
        {
            event.setCanceled(true);
            if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK)
            {
                new LookupTask(event.entityPlayer).setLocation(event.x, event.y, event.z).go();
            }
            else if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
            {
                int x = event.x, y = event.y, z = event.z;
                D3Log.getLogger().debug(event.face);
                switch (event.face)
                {
                    case 0:
                        y --;
                        break;
                    case 1:
                        y ++;
                        break;
                    case 2:
                        z --;
                        break;
                    case 3:
                        z ++;
                        break;
                    case 4:
                        x --;
                        break;
                    case 5:
                        x ++;
                        break;
                    default:
                        D3Log.getLogger().debug("Face not 0 - 5? {}", event.face);
                        return;
                }
                new LookupTask(event.entityPlayer).setLocation(x, y, z).go();
            }
        }
    }
}
