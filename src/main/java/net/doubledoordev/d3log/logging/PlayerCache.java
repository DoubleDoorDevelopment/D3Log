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

import com.mojang.authlib.GameProfile;
import net.doubledoordev.d3log.util.UserProfile;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Dries007
 */
public class PlayerCache
{
    public static final LinkedBlockingQueue<UserProfile> TO_ADD_USER_PROFILES = new LinkedBlockingQueue<>();
    private static final HashMap<UUID, UserProfile> UUID_USER_PROFILE_HASH_MAP = new HashMap<>();
    private static final HashMap<Integer, UserProfile> ID_USER_PROFILE_HASH_MAP = new HashMap<>();

    private PlayerCache()
    {
    }

    public static boolean hasIDFor(UUID uuid)
    {
        return UUID_USER_PROFILE_HASH_MAP.containsKey(uuid);
    }

    public static UserProfile getFromUUID(UUID uuid)
    {
        return UUID_USER_PROFILE_HASH_MAP.get(uuid);
    }

    public static UserProfile getFromInt(int anInt)
    {
        return ID_USER_PROFILE_HASH_MAP.get(anInt);
    }

    public static void addCompleteProfile(UserProfile userProfile)
    {
        UUID_USER_PROFILE_HASH_MAP.put(userProfile.getUuid(), userProfile);
        ID_USER_PROFILE_HASH_MAP.put(userProfile.getId(), userProfile);
    }

    public static void login(GameProfile gameProfile)
    {
        if (!hasIDFor(gameProfile.getId()))
        {
            UserProfile userProfile = new UserProfile(gameProfile);
            if (!TO_ADD_USER_PROFILES.contains(userProfile)) TO_ADD_USER_PROFILES.add(new UserProfile(gameProfile));
        }
        else
        {
            UserProfile profile = getFromUUID(gameProfile.getId());
            profile.setUsername(gameProfile.getName());
        }
    }
}
