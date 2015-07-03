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

import com.mojang.authlib.GameProfile;
import net.doubledoordev.d3log.logging.PlayerCache;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Dries007
 */
public class UserProfile
{
    private final UUID uuid;
    private String username = "";
    private int id = -1;

    public UserProfile(GameProfile profile)
    {
        this.uuid = profile.getId();
        this.username = profile.getName();
    }

    public UserProfile(UUID uuid, String username, int id)
    {
        this.uuid = uuid;
        this.username = username;
        this.id = id;
    }

    public UserProfile(UUID uuid)
    {
        this.uuid = uuid;
    }

    public void setId(int id)
    {
        this.id = id;
        PlayerCache.addCompleteProfile(this);
    }

    public void setUsername(String username)
    {
        if (!Objects.equals(this.username, username))
        {
            this.username = username;
            PlayerCache.TO_ADD_USER_PROFILES.add(this);
        }
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public String getUsername()
    {
        return username;
    }

    public int getId()
    {
        return id;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserProfile profile = (UserProfile) o;

        return uuid.equals(profile.uuid);

    }

    @Override
    public int hashCode()
    {
        return uuid.hashCode();
    }
}
