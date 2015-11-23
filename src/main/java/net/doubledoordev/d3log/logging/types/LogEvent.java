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

package net.doubledoordev.d3log.logging.types;

import net.doubledoordev.d3log.D3Log;
import net.doubledoordev.d3log.logging.TypeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

/**
 * @author Dries007
 */
public class LogEvent
{
    protected int epoch;
    protected int x, y, z, dim;
    protected String data;
    protected UUID uuid;
    protected int id; // The id of the event in the database.
    protected boolean ignored;

    {
        epoch = (int) (System.currentTimeMillis() / 1000);
    }

    private TypeRegistry.EventType type;

    public static ItemStack copy(ItemStack item)
    {
        return item == null ? null : item.copy();
    }

    public static ItemStack[] copy(ItemStack... item)
    {
        ItemStack[] stacks = new ItemStack[item.length];
        for (int i = 0; i < item.length; i++) stacks[i] = copy(item[i]);
        return stacks;
    }

    public void save()
    {

    }

    public void load()
    {

    }

    public void setPosition(Entity entity)
    {
        if (entity != null)
        {
            dim = entity.dimension;
            x = MathHelper.floor_double(entity.posX);
            y = MathHelper.floor_double(entity.posY + 0.5D);
            z = MathHelper.floor_double(entity.posZ);
        }
    }

    public void setPosition(int dimension, int x, int y, int z)
    {
        this.dim = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setPlayerPosAndUuid(EntityPlayer player)
    {
        setPosition(player);
        setPlayerUUID(player);
    }

    public void setPlayerUUID(EntityPlayer player)
    {
        if (player != null)
        {
            ignored = D3Log.getConfig().isIgnored(player);
            uuid = player.getGameProfile().getId();
        }
    }

    public long getEpoch()
    {
        return epoch;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public int getZ()
    {
        return z;
    }

    public int getDim()
    {
        return dim;
    }

    public String getData()
    {
        return data;
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public int getTypeId()
    {
        return type.getId();
    }

    public boolean isIgnored()
    {
        return ignored;
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public void setType(String type)
    {
        this.type = TypeRegistry.TYPE_REGISTRY.get(type);
    }

    public void setType(int id)
    {
        this.type = TypeRegistry.TYPE_REGISTRY.get(id);
    }

    public void setUuid(UUID uuid)
    {
        this.uuid = uuid;
    }

    public void setPosition(int dim, ChunkPosition position)
    {
        this.dim = dim;
        this.x = position.chunkPosX;
        this.y = position.chunkPosY;
        this.z = position.chunkPosZ;
    }

    public void setID(int id)
    {
        this.id = id;
    }

    public void setEpoch(int epoch)
    {
        this.epoch = epoch;
    }

    public int getID()
    {
        return id;
    }

    public void rollback()
    {

    }

    public void setType(TypeRegistry.EventType type)
    {
        this.type = type;
    }

    public TypeRegistry.EventType getType()
    {
        return type;
    }
}
