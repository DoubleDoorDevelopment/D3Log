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

import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.BlockSnapshot;

import static net.doubledoordev.d3log.util.Constants.GSON;

/**
 * @author Dries007
 */
public class ExplosionDamageLogEvent extends LogEvent
{
    protected ExplosionData explosionData;

    @Override
    public void save()
    {
        this.data = explosionData == null ? null : GSON.toJson(explosionData);
    }

    @Override
    public void load()
    {
        if (this.data == null) return;
        this.explosionData = GSON.fromJson(this.data, ExplosionData.class);
    }

    public void setSource(int x, int y, int z)
    {
        if (explosionData == null) explosionData = new ExplosionData();
        this.explosionData.srcX = x;
        this.explosionData.srcY = y;
        this.explosionData.srcZ = z;
    }

    public void makeSnapshot(World world)
    {
        if (explosionData == null) explosionData = new ExplosionData();
        this.explosionData.snapshot = new BlockSnapshot(world, this.x, this.y, this.z, world.getBlock(this.x, this.y, this.z), world.getBlockMetadata(this.x, this.y, this.z));
    }

    public static class ExplosionData
    {
        public BlockSnapshot snapshot;
        public int srcX, srcY, srcZ;
    }

    @Override
    public void rollback()
    {
        if (explosionData == null) return;
        explosionData.snapshot.world = DimensionManager.getWorld(explosionData.snapshot.dimId);
        explosionData.snapshot.restore(true);
    }
}
