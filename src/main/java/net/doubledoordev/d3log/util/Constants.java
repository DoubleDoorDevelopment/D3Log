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

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cpw.mods.fml.common.registry.GameRegistry;
import net.doubledoordev.d3log.util.json.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;

import java.lang.reflect.Type;

/**
 * @author Dries007
 */
public class Constants
{
    // MOD STUFF

    public static final String MODID = "D3Log";

    public static final Gson GSON = getGsonBuilder().create();
    public static final Gson GSON_PP = getGsonBuilder().setPrettyPrinting().create();

    // TYPES

    public static final String TYPE_COMMAND = "command";
    public static final String TYPE_CHAT = "chat";
    public static final String TYPE_LOGIN = "login";
    public static final String TYPE_LOGOUT = "logout";
    public static final String TYPE_RESPAWN = "respawn";
    public static final String TYPE_CHANGE_DIM = "changeDim";
    public static final String TYPE_ITEM_PICKUP = "itemPickup";
    public static final String TYPE_ITEM_CRAFTING = "itemCrafting";
    public static final String TYPE_ITEM_SMELTING = "itemSmelting";
    public static final String TYPE_ITEM_TOSS = "itemToss";
    public static final String TYPE_DAMAGE_GOT = "damageGot";
    public static final String TYPE_DAMAGE_DEALT = "damageDealt";
    public static final String TYPE_KILLED = "killed";
    public static final String TYPE_DIED = "died";
    public static final String TYPE_DROPS = "drops";
//    public static final String TYPE_ACHIEVEMENT = "achievement";
    public static final String TYPE_ANVIL_REPAIR = "anvilRepair";
    public static final String TYPE_BONEMEAL = "bonemeal";
    public static final String TYPE_INTERACT_ENTITY = "interactEntity";
    public static final String TYPE_INTERACT_WORLD = "interactWorld";
    public static final String TYPE_FILL_BUCKET = "fillBucket";
    public static final String TYPE_SLEEP = "sleep";
    public static final String TYPE_BLOCK_BREAK = "blockBreak";
    public static final String TYPE_BLOCK_PLACE = "blockPlace";
    public static final String TYPE_EXPLOSION_SOURCE = "explosionSource";
    public static final String TYPE_EXPLOSION_DAMAGE = "explosionDamage";

    public static final Type NBT_COMPOUND_TYPE = TypeToken.get(NBTTagCompound.class).getType();

    public static final ItemStack WAND = getWand();

    public static final Joiner JOINER_AND = Joiner.on(" AND ");
    public static final Joiner JOINER_SPACE = Joiner.on(' ');

    private static ItemStack getWand()
    {
        ItemStack wand = new ItemStack(Blocks.command_block);

        wand.setStackDisplayName(MODID + " Wand");
        wand.setTagInfo(MODID, new NBTTagByte((byte) 1));
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString("Right click for events where the block would be placed."));
        list.appendTag(new NBTTagString("Left click for events for the block you click on."));
        wand.getTagCompound().getCompoundTag("display").setTag("Lore", list);

        return wand;
    }

    private static GsonBuilder getGsonBuilder()
    {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.disableHtmlEscaping();
        gsonBuilder.registerTypeHierarchyAdapter(Entity.class, new EntityJson());
        gsonBuilder.registerTypeHierarchyAdapter(TileEntity.class, new TileEntityJson());
        gsonBuilder.registerTypeHierarchyAdapter(ItemStack.class, new ItemStackJson());
        gsonBuilder.registerTypeHierarchyAdapter(NBTBase.class, new JsonNbt());
        gsonBuilder.registerTypeHierarchyAdapter(ChunkPosition.class, new ChunkPositionJson());
        gsonBuilder.registerTypeHierarchyAdapter(GameRegistry.UniqueIdentifier.class, new UniqueIdentifierJson());

        return gsonBuilder;
    }

}
