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
import net.doubledoordev.d3log.logging.types.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;

import java.util.ArrayList;
import java.util.ListIterator;

import static net.doubledoordev.d3log.util.Constants.*;

/**
 * @author Dries007
 */
public class ForgeEventHandlers
{
    public static final ForgeEventHandlers FORGE_EVENT_HANDLERS = new ForgeEventHandlers();

    private ForgeEventHandlers()
    {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void itemTossEvent(ItemTossEvent event)
    {
        ItemsLogEvent log = new ItemsLogEvent();

        log.setType(TYPE_ITEM_TOSS);
        log.setPlayerPosAndUuid(event.player);
        log.setData(event.entityItem);

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void livingHurtEvent(LivingHurtEvent event)
    {
        // Player got hurt
        if (event.entityLiving instanceof EntityPlayer)
        {
            DamageLogEvent log = new DamageLogEvent();

            log.setType(TYPE_DAMAGE_GOT);
            log.setTarget(event.entityLiving);
            log.setAmount(event.ammount);
            log.setDamageSource(event.source);

            log.setPlayerPosAndUuid((EntityPlayer) event.entityLiving);

            LoggingQueue.addToQueue(log);
        }
        // Player hurt an entity
        if (event.source.getEntity() instanceof EntityPlayer)
        {
            DamageLogEvent log = new DamageLogEvent();

            log.setType(TYPE_DAMAGE_DEALT);
            log.setTarget(event.entityLiving);
            log.setAmount(event.ammount);
            log.setDamageSource(event.source);

            log.setPlayerPosAndUuid((EntityPlayer) event.source.getEntity());

            LoggingQueue.addToQueue(log);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void livingDeathEvent(LivingDeathEvent event)
    {
        // Player died
        if (event.entityLiving instanceof EntityPlayer)
        {
            DamageLogEvent log = new DamageLogEvent();

            log.setType(TYPE_DIED);
            log.setTarget(event.entityLiving);
            log.setDamageSource(event.source);

            log.setPlayerPosAndUuid((EntityPlayer) event.entityLiving);

            LoggingQueue.addToQueue(log);
        }
        // Player killed
        if (event.source.getEntity() instanceof EntityPlayer)
        {
            DamageLogEvent log = new DamageLogEvent();

            log.setType(TYPE_KILLED);
            log.setTarget(event.entityLiving);
            log.setDamageSource(event.source);

            log.setPlayerPosAndUuid((EntityPlayer) event.source.getEntity());

            LoggingQueue.addToQueue(log);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerDropsEvent(PlayerDropsEvent event)
    {
        if (event.drops.isEmpty()) return;
        ItemsLogEvent log = new ItemsLogEvent();

        log.setType(TYPE_DROPS);
        log.setPlayerPosAndUuid(event.entityPlayer);
        log.setData(event.drops);

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void entityItemPickupEvent(EntityItemPickupEvent event)
    {
        ItemsLogEvent log = new ItemsLogEvent();

        log.setType(TYPE_ITEM_PICKUP);
        log.setPlayerPosAndUuid(event.entityPlayer);
        log.setData(event.item);

        LoggingQueue.addToQueue(log);
    }

    // BUGGED: triggers multiple times.
//    @SubscribeEvent(priority = EventPriority.LOWEST)
//    public void achievementEvent(AchievementEvent event)
//    {
//        LogEvent log = new LogEvent();
//
//        log.setType(TYPE_ACHIEVEMENT);
//        log.setPlayerPosAndUuid(event.entityPlayer);
//        log.setData(event.achievement.statId);
//
//        LoggingQueue.addToQueue(log);
//    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void anvilRepairEvent(AnvilRepairEvent event)
    {
        ItemsLogEvent log = new ItemsLogEvent();

        log.setType(TYPE_ANVIL_REPAIR);
        log.setPlayerPosAndUuid(event.entityPlayer);
        log.setData(event.left, event.right, event.output);

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void bonemealEvent(BonemealEvent event)
    {
        LogEvent log = new LogEvent();

        log.setType(TYPE_BONEMEAL);
        log.setPlayerUUID(event.entityPlayer);
        log.setPosition(event.world.provider.dimensionId, event.x, event.y, event.z);

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void entityInteractEvent(EntityInteractEvent event)
    {
        EntityInteractLogEvent log = new EntityInteractLogEvent();

        log.setType(TYPE_INTERACT_ENTITY);
        log.setPlayerPosAndUuid(event.entityPlayer);
        log.setData(event.target, event.entityPlayer.getCurrentEquippedItem());

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerInteractEvent(PlayerInteractEvent event)
    {
        WorldInteractLogEvent log = new WorldInteractLogEvent();

        log.setType(TYPE_INTERACT_WORLD);
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR)
        {
            log.setPlayerPosAndUuid(event.entityPlayer);
        }
        else
        {
            log.setPlayerUUID(event.entityPlayer);
            log.setPosition(event.entity.dimension, event.x, event.y, event.z);
            log.setData(event.action, event.face, event.entityPlayer.getCurrentEquippedItem());
        }

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void fillBucketEvent(FillBucketEvent event)
    {
        FillBucketLogEvent log = new FillBucketLogEvent();

        log.setType(TYPE_FILL_BUCKET);
        log.setPlayerPosAndUuid(event.entityPlayer);
        log.setData(event.current, event.result, event.target);

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerSleepInBedEvent(PlayerSleepInBedEvent event)
    {
        LogEvent log = new LogEvent();

        log.setType(TYPE_SLEEP);
        log.setPlayerPosAndUuid(event.entityPlayer);
        log.setData(GSON.toJson(event.result));

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void blockBreakEvent(BlockEvent.BreakEvent event)
    {
        BlockSnapshotLogEvent log = new BlockSnapshotLogEvent();

        log.setType(TYPE_BLOCK_BREAK);
        log.setPlayerUUID(event.getPlayer());
        log.setPosition(event.world.provider.dimensionId, event.x, event.y, event.z);
        NBTTagCompound nbt = null;
        if (event.block.hasTileEntity(event.blockMetadata))
        {
            TileEntity te = event.world.getTileEntity(event.x, event.y, event.z);
            if (te != null)
            {
                nbt = new NBTTagCompound();
                te.writeToNBT(nbt);
            }
        }
        log.setData(new BlockSnapshot(event.world, event.x, event.y, event.z, event.block, event.blockMetadata, nbt));

        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void blockPlaceEvent(BlockEvent.PlaceEvent event)
    {
        BlockPlaceLogEvent log = new BlockPlaceLogEvent();

        log.setType(TYPE_BLOCK_PLACE);
        log.setPlayerUUID(event.player);
        log.setPosition(event.world.provider.dimensionId, event.x, event.y, event.z);
        log.setItemHolding(event.itemInHand);
        NBTTagCompound nbt = null;
        if (event.block.hasTileEntity(event.blockMetadata))
        {
            TileEntity te = event.world.getTileEntity(event.x, event.y, event.z);
            if (te != null)
            {
                nbt = new NBTTagCompound();
                te.writeToNBT(nbt);
            }
        }
        log.setAfter(new BlockSnapshot(event.world, event.x, event.y, event.z, event.block, event.blockMetadata, nbt));
        if (event instanceof BlockEvent.MultiPlaceEvent)
        {
             log.setBefore(((BlockEvent.MultiPlaceEvent) event).getReplacedBlockSnapshots().toArray(new BlockSnapshot[((BlockEvent.MultiPlaceEvent) event).getReplacedBlockSnapshots().size()]));
        }
        else
        {
            log.setBefore(event.blockSnapshot);
        }
        LoggingQueue.addToQueue(log);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void explosionEvent(ExplosionEvent.Detonate event)
    {
        int dim = event.world.provider.dimensionId;
        int x = MathHelper.floor_double(event.explosion.explosionX);
        int y = MathHelper.floor_double(event.explosion.explosionY + 0.5D);
        int z = MathHelper.floor_double(event.explosion.explosionZ);
        EntityLivingBase source = event.explosion.getExplosivePlacedBy();
        boolean player = source instanceof EntityPlayer;

        ArrayList<ChunkPosition> positions = new ArrayList<>(event.getAffectedBlocks());
        // Remove air
        {
            ListIterator<ChunkPosition> i = positions.listIterator();
            while (i.hasNext())
            {
                ChunkPosition next = i.next();
                if (event.world.isAirBlock(next.chunkPosX, next.chunkPosY, next.chunkPosZ)) i.remove();
            }
        }

        // Explosion Source
        {
            ExplosionSourceLogEvent log = new ExplosionSourceLogEvent();

            log.setType(TYPE_EXPLOSION_SOURCE);
            if (player) log.setPlayerUUID((EntityPlayer) source);
            log.setPosition(dim, x, y, z);
            log.setExplosionProperties(event.explosion);
            log.setAffectedEntities(event.getAffectedEntities());
            log.setAffectedBlocks(positions);

            LoggingQueue.addToQueue(log);
        }

        // Blocks hit by Explosion
        for (ChunkPosition position : positions)
        {
            ExplosionDamageLogEvent log = new ExplosionDamageLogEvent();

            log.setType(TYPE_EXPLOSION_DAMAGE);
            if (player) log.setPlayerUUID((EntityPlayer) source);
            log.setPosition(dim, position);
            log.setSource(x, y, z);
            log.makeSnapshot(event.world);

            LoggingQueue.addToQueue(log);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void commandEvent(CommandEvent event)
    {
        if (event.command instanceof EntityPlayer)
        {
            LogEvent log = new LogEvent();

            log.setType(TYPE_COMMAND);
            log.setPlayerPosAndUuid((EntityPlayer) event.command);
            log.setData(event.command.getCommandName() + " " + JOINER_SPACE.join(event.parameters));

            LoggingQueue.addToQueue(log);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void serverChatEvent(ServerChatEvent event)
    {
        LogEvent log = new LogEvent();

        log.setType(TYPE_CHAT);
        log.setPlayerPosAndUuid(event.player);
        log.setData(event.message);

        LoggingQueue.addToQueue(log);
    }
}
