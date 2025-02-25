package net.modfest.scatteredshards.client.render;

import net.modfest.scatteredshards.api.shard.ShardType;
import net.modfest.scatteredshards.client.ScatteredShardsClient;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Quaternionf;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.modfest.scatteredshards.block.ShardBlockEntity;
import net.modfest.scatteredshards.api.shard.Shard;

@ClientOnly
public class ShardBlockEntityRenderer implements BlockEntityRenderer<ShardBlockEntity> {
	public static final float TICKS_PER_RADIAN = 16f;

	public ShardBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {

	}


	@Override
	public void render(ShardBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {

		Shard shard = entity.getShard();
		if (shard == null) {
			//Let's make one up!
			shard = Shard.MISSING_SHARD;
		}

		ShardType shardType = shard.getShardType();

		float l = (ScatteredShardsClient.animationTickCounter + tickDelta) / TICKS_PER_RADIAN;
		l %= Math.PI*2;
		float angle = l; //(float) (Math.PI/8);
		Quaternionf rot = new Quaternionf(new AxisAngle4f(angle, 0f, 1f, 0f));
		Quaternionf tilt = new Quaternionf(new AxisAngle4f((float) (Math.PI/8), 0f, 0f, 1f));

		matrices.push();

		matrices.translate(0.5, 0.5, 0.5);
		matrices.multiply(rot);
		matrices.multiply(tilt);

		VertexConsumer buf = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(shardType.getBackingTexture()));
		
		/*
		 * A note about scale here:
		 * 0.75m or 24/32 == 32px on the card face.
		 * The card is 24 x 32, meaning it's 0.75 x as wide as it is tall.
		 * 0.75 x 0.75 == 0.5625 or 18/32 is the card's proper width
		 */
		float cardHeight = 24/32f;
		float cardWidth = 18/32f;
		
		float halfHeight = cardHeight / 2f;
		float halfWidth = cardWidth / 2f;
		
		Vector3f dl = new Vector3f(-halfWidth, -halfHeight, 0f);
		Vector3f dr = new Vector3f( halfWidth, -halfHeight, 0f);
		Vector3f ul = new Vector3f(-halfWidth,  halfHeight, 0f);
		Vector3f ur = new Vector3f( halfWidth,  halfHeight, 0f);

		Vector3f normal = new Vector3f(0, 0, -1);

		//Draw card back
		buf
			.vertex(matrices.peek().getModel(), dl.x, dl.y, dl.z)
			.color(0xFF_FFFFFF)
			.uv(0, 1)
			.overlay(overlay)
			.light(light)
			.normal(normal.x(), normal.y(), normal.z())
			.next();

		buf
			.vertex(matrices.peek().getModel(), dr.x, dr.y, dr.z)
			.color(0xFF_FFFFFF)
			.uv(1, 1)
			.overlay(overlay)
			.light(light)
			.normal(normal.x(), normal.y(), normal.z())
			.next();

		buf
			.vertex(matrices.peek().getModel(), ur.x, ur.y, ur.z)
			.color(0xFF_FFFFFF)
			.uv(1, 0)
			.overlay(overlay)
			.light(light)
			.normal(normal.x(), normal.y(), normal.z())
			.next();

		buf
			.vertex(matrices.peek().getModel(), ul.x, ul.y, ul.z)
			.color(0xFF_FFFFFF)
			.uv(0, 0)
			.overlay(overlay)
			.light(light)
			.normal(normal.x(), normal.y(), normal.z())
			.next();

		//Draw card front
		Vector3f revNormal = normal.mul(-1, -1, -1);
		buf = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(shardType.getFrontTexture()));
		buf
			.vertex(matrices.peek().getModel(), dl.x, dl.y, dl.z)
			.color(0xFF_FFFFFF)
			.uv(1, 1)
			.overlay(overlay)
			.light(light)
			.normal(matrices.peek().getNormal(), revNormal.x(), revNormal.y(), revNormal.z())
			.next();

		buf
			.vertex(matrices.peek().getModel(), ul.x, ul.y, ul.z)
			.color(0xFF_FFFFFF)
			.uv(1, 0)
			.overlay(overlay)
			.light(light)
			.normal(matrices.peek().getNormal(), revNormal.x(), revNormal.y(), revNormal.z())
			.next();

		buf
			.vertex(matrices.peek().getModel(), ur.x, ur.y, ur.z)
			.color(0xFF_FFFFFF)
			.uv(0, 0)
			.overlay(overlay)
			.light(light)
			.normal(matrices.peek().getNormal(), revNormal.x(), revNormal.y(), revNormal.z())
			.next();

		buf
			.vertex(matrices.peek().getModel(), dr.x, dr.y, dr.z)
			.color(0xFF_FFFFFF)
			.uv(0, 1)
			.overlay(overlay)
			.light(light)
			.normal(matrices.peek().getNormal(), revNormal.x(), revNormal.y(), revNormal.z())
			.next();
		
		/*
		 * Another note about scale:
		 * because there are a different number of texels in each dimension, we need a separate pixel ratio for width
		 * and height:
		 * 
		 * For width, 1.0 equates to 24px, so the ratio is 1/24f
		 * For height, 1.0 equates to 32px, so the ratio is 1/32f
		 * 
		 * To translate this into distance units, we multiply by the size of the card in meters in that dimensions.
		 * 
		 * Once that's done, with our {4, 4, 4, 12} insets, we arrive at a perfect 16px x 16px square
		 * (in card-image pixels) for the card-icon texture
		 * 
		 */
		float xpx = 1/24f * cardWidth;
		float ypx = 1/32f * cardHeight;

		shard.icon().ifLeft( stack -> {
			matrices.translate(0, 4*ypx, -0.01f); //extra -0.002 here to prevent full-cubes from zfighting the card
			matrices.scale(0.5f, 0.5f, 0.01f /*0.6f*/);
			

			MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
		});

		shard.icon().ifRight( texId -> {
			VertexConsumer v = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(texId));

			v.vertex(matrices.peek().getModel(), dl.x + (4*xpx), dl.y + (12*ypx), dl.z - 0.002f)
				.color(0xFF_FFFFFF)
				.uv(1, 1)
				.overlay(overlay)
				.light(light)
				.normal(matrices.peek().getNormal(), revNormal.x(), revNormal.y(), revNormal.z())
				.next();

			v.vertex(matrices.peek().getModel(), ul.x + (4*xpx), ul.y - (4*ypx), ul.z - 0.002f)
				.color(0xFF_FFFFFF)
				.uv(1, 0)
				.overlay(overlay)
				.light(light)
				.normal(matrices.peek().getNormal(), revNormal.x(), revNormal.y(), revNormal.z())
				.next();

			v.vertex(matrices.peek().getModel(), ur.x - (4*xpx), ur.y - (4*ypx), ur.z - 0.002f)
				.color(0xFF_FFFFFF)
				.uv(0, 0)
				.overlay(overlay)
				.light(light)
				.normal(matrices.peek().getNormal(), revNormal.x(), revNormal.y(), revNormal.z())
				.next();

			v.vertex(matrices.peek().getModel(), dr.x - (4*xpx), dr.y + (12*ypx), dr.z - 0.002f)
				.color(0xFF_FFFFFF)
				.uv(0, 1)
				.overlay(overlay)
				.light(light)
				.normal(matrices.peek().getNormal(), revNormal.x(), revNormal.y(), revNormal.z())
				.next();
		});

		matrices.pop();

	}
}
