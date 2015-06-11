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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

import static net.doubledoordev.d3log.util.Constants.GSON;

/**
 * @author Dries007
 */
public class DamageLogEvent extends LogEvent
{
    protected DamageData damageData;

    public void setTarget(EntityLivingBase entityLiving)
    {
        if (damageData == null) damageData = new DamageData();
        damageData.target = entityLiving;
    }

    public void setAmount(float amount)
    {
        if (damageData == null) damageData = new DamageData();
        damageData.amount = amount;
    }

    public void setDamageSource(DamageSource source)
    {
        if (damageData == null) damageData = new DamageData();
        damageData.damageType = source.getDamageType();

        Entity directSource = source.getSourceOfDamage();
        Entity inDirectSource = source.getEntity();

        if (directSource != null || inDirectSource != null)
        {
            damageData.source = directSource;

            if (directSource != inDirectSource) damageData.indirectSource = inDirectSource;
        }
    }

    @Override
    public void save()
    {
        this.data = damageData == null ? null : GSON.toJson(damageData);
    }

    public static class DamageData
    {
        public float amount;
        public String damageType;
        public Entity target;
        public Entity source;
        public Entity indirectSource;
    }
}
