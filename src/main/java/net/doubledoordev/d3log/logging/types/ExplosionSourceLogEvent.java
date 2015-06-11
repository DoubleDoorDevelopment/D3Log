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
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;

import java.util.List;

import static net.doubledoordev.d3log.util.Constants.GSON;

/**
 * @author Dries007
 */
public class ExplosionSourceLogEvent extends LogEvent
{
    protected ExplosionData explosionData;

    @Override
    public void save()
    {
        this.data = explosionData == null ? null : GSON.toJson(explosionData);
    }

    public void setAffectedEntities(List<Entity> affectedEntities)
    {
        if (this.explosionData == null) this.explosionData = new ExplosionData();
        this.explosionData.entities = affectedEntities;
    }

    public void setAffectedBlocks(List<ChunkPosition> affectedBlocks)
    {
        if (this.explosionData == null) this.explosionData = new ExplosionData();
        this.explosionData.blocks = affectedBlocks;
    }

    public void setExplosionProperties(Explosion explosion)
    {
        if (this.explosionData == null) this.explosionData = new ExplosionData();
        this.explosionData.source = explosion.getExplosivePlacedBy();
        this.explosionData.explosionSize = explosion.explosionSize;
    }

    public static class ExplosionData
    {
        public List<ChunkPosition> blocks;
        public List<Entity> entities;
        public EntityLivingBase source;
        public float explosionSize;
    }
}
