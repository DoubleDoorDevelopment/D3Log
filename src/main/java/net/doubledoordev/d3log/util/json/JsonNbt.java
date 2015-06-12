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

package net.doubledoordev.d3log.util.json;

import com.google.gson.*;
import net.minecraft.nbt.*;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraftforge.common.util.Constants.NBT.*;

/**
* @author Dries007
*/
public final class JsonNbt implements JsonSerializer<NBTBase>, JsonDeserializer<NBTBase>
{
    private static final Pattern BYTE = Pattern.compile("([-+]?[0-9]+)b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LONG = Pattern.compile("([-+]?[0-9]+)l", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT = Pattern.compile("([-+]?[0-9]+)s", Pattern.CASE_INSENSITIVE);
    private static final Pattern BYTE_ARRAY = Pattern.compile("b\\[((?:(?:[-+]?[0-9]+), ?)*(?:[-+]?[0-9]+))\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTEGER_ARRAY = Pattern.compile("i?\\[((?:(?:[-+]?[0-9]+), ?)*(?:[-+]?[0-9]+))\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOAT = Pattern.compile("([-+]?[0-9]*\\.?[0-9]+)f", Pattern.CASE_INSENSITIVE);// Parse before DOUBLE
    private static final Pattern DOUBLE = Pattern.compile("([-+]?[0-9]*\\.?[0-9]+)d?", Pattern.CASE_INSENSITIVE);// Parse after FLOAT
    private static final Pattern INTEGER = Pattern.compile("([-+]?[0-9]+)i?", Pattern.CASE_INSENSITIVE);// Parse LAST

    @Override
    public NBTBase deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        if (json.isJsonNull()) return null;
        if (json.isJsonPrimitive()) return getNBTPrimitive(json.getAsJsonPrimitive());
        if (json.isJsonObject()) return getNBTTagCompound(context, json.getAsJsonObject());
        if (json.isJsonArray()) return getNBTTagArray(context, json.getAsJsonArray());

        throw new IllegalArgumentException(json.toString());
    }

    private NBTTagList getNBTTagArray(JsonDeserializationContext context, JsonArray json)
    {
        NBTTagList tagList = new NBTTagList();

        for (JsonElement element : json)
        {
            tagList.appendTag(context.<NBTBase>deserialize(element, NBTBase.class));
        }

        return tagList;
    }

    private NBTTagCompound getNBTTagCompound(JsonDeserializationContext context, JsonObject json)
    {
        NBTTagCompound tagCompound = new NBTTagCompound();

        for (Map.Entry<String, JsonElement> entry : json.entrySet())
        {
            tagCompound.setTag(entry.getKey(), context.<NBTBase>deserialize(entry.getValue(), NBTBase.class));
        }

        return tagCompound;
    }

    private NBTBase getNBTPrimitive(JsonPrimitive json)
    {
        String value = json.getAsString();

        Matcher matcher = BYTE.matcher(value);
        if (matcher.matches()) return new NBTTagByte(Byte.parseByte(matcher.group(1)));

        matcher = LONG.matcher(value);
        if (matcher.matches()) return new NBTTagLong(Long.parseLong(matcher.group(1)));

        matcher = SHORT.matcher(value);
        if (matcher.matches()) return new NBTTagShort(Short.parseShort(matcher.group(1)));

        matcher = FLOAT.matcher(value);
        if (matcher.matches()) return new NBTTagFloat(Float.parseFloat(matcher.group(1)));

        matcher = DOUBLE.matcher(value);
        if (matcher.matches()) return new NBTTagDouble(Double.parseDouble(matcher.group(1)));

        matcher = INTEGER.matcher(value);
        if (matcher.matches()) return new NBTTagInt(Integer.parseInt(matcher.group(1)));

        matcher = BYTE_ARRAY.matcher(value);
        if (matcher.matches())
        {
            String[] split = matcher.group(1).split(", ?");
            byte[] data = new byte[split.length];
            for (int i = 0; i < split.length; i++) data[i] = Byte.parseByte(split[i]);
            return new NBTTagByteArray(data);
        }

        matcher = INTEGER_ARRAY.matcher(value);
        if (matcher.matches())
        {
            String[] split = matcher.group(1).split(", ?");
            int[] data = new int[split.length];
            for (int i = 0; i < split.length; i++) data[i] = Integer.parseInt(split[i]);
            return new NBTTagIntArray(data);
        }

        if (value.equalsIgnoreCase("false")) return new NBTTagByte((byte) 0);
        if (value.equalsIgnoreCase("true")) return new NBTTagByte((byte) 1);

        return new NBTTagString(value);
    }

    @Override
    public JsonElement serialize(NBTBase src, Type typeOfSrc, JsonSerializationContext context)
    {
        switch (src.getId())
        {
            case TAG_COMPOUND:
                return getJSONObject(context, ((NBTTagCompound) src));
            case TAG_LIST:
                return getJSONArray(context, ((NBTTagList) src));
            case TAG_END:
                return JsonNull.INSTANCE;
            case TAG_BYTE:
                return new JsonPrimitive(((NBTBase.NBTPrimitive) src).func_150290_f() + "b");
            case TAG_SHORT:
                return new JsonPrimitive(((NBTBase.NBTPrimitive) src).func_150289_e() + "s");
            case TAG_INT:
                return new JsonPrimitive(((NBTBase.NBTPrimitive) src).func_150287_d() + "i");
            case TAG_LONG:
                return new JsonPrimitive(((NBTBase.NBTPrimitive) src).func_150291_c() + "l");
            case TAG_FLOAT:
                return new JsonPrimitive(((NBTBase.NBTPrimitive) src).func_150288_h() + "f");
            case TAG_DOUBLE:
                return new JsonPrimitive(((NBTBase.NBTPrimitive) src).func_150286_g() + "d");
            case TAG_STRING:
                return new JsonPrimitive(((NBTTagString) src).func_150285_a_());
            case TAG_BYTE_ARRAY:
                return new JsonPrimitive("b" + Arrays.toString(((NBTTagByteArray) src).func_150292_c()));
            case TAG_INT_ARRAY:
                return new JsonPrimitive("i" + Arrays.toString(((NBTTagIntArray) src).func_150302_c()));
        }

        throw new IllegalArgumentException(src.toString());
    }

    private JsonArray getJSONArray(JsonSerializationContext context, NBTTagList list)
    {
        JsonArray array = new JsonArray();
        for (int i = 0; i < list.tagCount(); i++)
        {
            array.add(context.serialize(list.tagList.get(i)));
        }
        return array;
    }

    private JsonObject getJSONObject(JsonSerializationContext context, NBTTagCompound compound)
    {
        JsonObject object = new JsonObject();
        //noinspection unchecked
        for (String key : (Set<String>) compound.func_150296_c())
        {
            object.add(key, context.serialize(compound.getTag(key)));
        }
        return object;
    }
}
