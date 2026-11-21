package zako.opt.cosmetic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.slf4j.LoggerFactory;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public final class BlenderCharacterMesh {
     private static final String[] PARTS = {"body", "head", "right_arm", "left_arm", "right_leg", "left_leg", "jeans"};
     private static final int STRIDE = 8;
     private static final int JEANS = 6;
     private static final int JEANS_STRIDE = 10;
     private static final float[][] MESH = load();
     private BlenderCharacterMesh() {}


    private static float[][] load() {
        try (var stream = BlenderCharacterMesh.class.getResourceAsStream("/assets/zakoopt/models/character/zako_mesh.json")) {
            if (stream == null) throw new IllegalStateException("Missing Blender character mesh");
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            int version = json.get("version").getAsInt();
            if ((version != 1 && version != 2) || json.get("stride").getAsInt() != STRIDE) throw new IllegalStateException("Unsupported character mesh format");
            JsonObject groups = json.getAsJsonObject("groups");
            float[][] result = new float[PARTS.length][];
            for (int i = 0; i < PARTS.length; i++) {
                if (i == JEANS && version == 1) continue;
                var values = groups.getAsJsonArray(PARTS[i]);
                int stride = i == JEANS ? JEANS_STRIDE : STRIDE;
                if (values == null || values.size() == 0 || values.size() % (stride * 3) != 0) throw new IllegalStateException("Invalid character part: " + PARTS[i]);
                result[i] = new float[values.size()];
                for (int j = 0; j < values.size(); j++) {
                    result[i][j] = values.get(j).getAsFloat();
                    if (!Float.isFinite(result[i][j])) throw new IllegalStateException("Invalid character vertex");
                }
                if (i == JEANS) {
                    for (int j = 0; j < result[i].length; j += stride) {
                        float right = result[i][j + 8], left = result[i][j + 9];
                        if (right < 0 || left < 0 || right + left > 1.00001f)
                            throw new IllegalStateException("Invalid jeans weights");
                    }
                }
            }
            return result;
        } catch (Exception e) {
            LoggerFactory.getLogger("zakoopt").error("Failed to load Blender character mesh", e);
            return null;
        }
    }

     public static boolean available() { return MESH != null; }
     public static int triangleCount() {
        if (MESH == null) return 0;
        int count = 0;
        for (int i = 0; i < MESH.length; i++) {
            if (MESH[i] != null) count += MESH[i].length / ((i == JEANS ? JEANS_STRIDE : STRIDE) * 3);
        }
        return count;
    }


    public static void submit(PlayerModel model, AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector,
                              RenderType type, int light, int overlay, int color) {
        model.setupAnim(state);
        ModelPart[] parts = {model.body, model.head, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        pose.pushPose();
        pose.translate(0, 1.5f, 0);
        pose.scale(1.15f, 1.15f, 1.15f);
        pose.translate(0, -1.5f, 0);
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].visible) continue;
            pose.pushPose();
            if (i == 2 || i == 3) pose.translate(armClearance(i, parts[i]), 0, 0);
            submitPart(i, parts[i], pose, collector, type, light, overlay, color);
            pose.popPose();
        }
        if (MESH[JEANS] != null && (model.body.visible || model.rightLeg.visible || model.leftLeg.visible))
            submitJeans(model, pose, collector, type, light, overlay, color);
        pose.popPose();
    }


    private static PoseStack.Pose jointTransform(ModelPart part) {
        PoseStack local = new PoseStack();
        part.translateAndRotate(local);
        // The exported jeans positions share the same bind space, including at the crotch seam.
        var bind = part.getInitialPose();
        local.translate(-bind.x() / 16f, -bind.y() / 16f, -bind.z() / 16f);
        return local.last();
    }


    private static void submitJeans(PlayerModel model, PoseStack pose, SubmitNodeCollector collector,
                                    RenderType type, int light, int overlay, int color) {
        PoseStack.Pose body = jointTransform(model.body);
        PoseStack.Pose right = jointTransform(model.rightLeg);
        PoseStack.Pose left = jointTransform(model.leftLeg);
        float[] data = MESH[JEANS];
        collector.submitCustomGeometry(pose, type, (matrix, vertices) -> {
            Vector3f position = new Vector3f(), normal = new Vector3f(), transformed = new Vector3f();
            for (int i = 0; i < data.length; i += JEANS_STRIDE) {
                float rightWeight = data[i + 8], leftWeight = data[i + 9];
                position.zero();
                normal.zero();
                blendJoint(body, 1f - rightWeight - leftWeight, data, i, position, normal, transformed);
                blendJoint(right, rightWeight, data, i, position, normal, transformed);
                blendJoint(left, leftWeight, data, i, position, normal, transformed);
                normal.normalize();
                jeansVertex(vertices, matrix, data, i, position, normal, light, overlay, color);
                if (i % (JEANS_STRIDE * 3) == JEANS_STRIDE * 2)
                    jeansVertex(vertices, matrix, data, i, position, normal, light, overlay, color);
            }
        });
    }


    private static void blendJoint(PoseStack.Pose joint, float weight, float[] data, int i,
                                   Vector3f position, Vector3f normal, Vector3f transformed) {
        if (weight <= 0) return;
        joint.pose().transformPosition(data[i], data[i + 1], data[i + 2], transformed);
        position.fma(weight, transformed);
        joint.normal().transform(data[i + 5], data[i + 6], data[i + 7], transformed);
        normal.fma(weight, transformed);
    }


    private static void jeansVertex(VertexConsumer vertex, PoseStack.Pose pose, float[] data, int i,
                                    Vector3fc position, Vector3fc normal, int light, int overlay, int color) {
        vertex.addVertex(pose, position.x(), position.y(), position.z()).setColor(color).setUv(data[i + 3], data[i + 4])
            .setOverlay(overlay).setLight(light).setNormal(pose, normal.x(), normal.y(), normal.z());
    }


    private static float armClearance(int index, ModelPart arm) {
        float side = index == 2 ? -1f : 1f;
        Quaternionf rotation = new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot);
        Vector3f point = new Vector3f();
        float offset = 0;
        float[] vertices = MESH[index];
        for (int i = 0; i < vertices.length; i += STRIDE) {
            point.set(vertices[i] * arm.xScale, vertices[i + 1] * arm.yScale, vertices[i + 2] * arm.zScale).rotate(rotation);
            float y = point.y + arm.y / 16f;
            if (y < .23f || y > .8f) continue;
            float height = Math.clamp((y - .16f) / .18f, 0f, 1f);
            float radiusX = (.32f + .10f * height);
            float depth = (point.z + arm.z / 16f + .025f) / .29f;
            if (Math.abs(depth) >= 1) continue;
            float edge = radiusX * (float) Math.sqrt(1 - depth * depth);
            offset = Math.max(offset, edge + .015f - side * (point.x + arm.x / 16f));
        }
        return side * Math.clamp(offset, 0f, .07f);
    }


    public static void submitArm(boolean right, ModelPart arm, PoseStack pose, SubmitNodeCollector collector,
                                 RenderType type, int light, int overlay) {
        submitPart(right ? 2 : 3, arm, pose, collector, type, light, overlay, -1);
    }


    private static void submitPart(int index, ModelPart part, PoseStack pose, SubmitNodeCollector collector,
                                   RenderType type, int light, int overlay, int color) {
        float[] data = MESH[index];
        pose.pushPose();
        part.translateAndRotate(pose);
        collector.submitCustomGeometry(pose, type, (matrix, vertices) -> {
            for (int i = 0; i < data.length; i += STRIDE * 3) {
                vertex(vertices, matrix, data, i, light, overlay, color);
                vertex(vertices, matrix, data, i + STRIDE, light, overlay, color);
                vertex(vertices, matrix, data, i + STRIDE * 2, light, overlay, color);
                vertex(vertices, matrix, data, i + STRIDE * 2, light, overlay, color);
            }
        });
        pose.popPose();
    }


    private static void vertex(VertexConsumer vertex, PoseStack.Pose pose, float[] d, int i, int light, int overlay, int color) {
        vertex.addVertex(pose, d[i], d[i + 1], d[i + 2]).setColor(color).setUv(d[i + 3], d[i + 4])
            .setOverlay(overlay).setLight(light).setNormal(pose, d[i + 5], d[i + 6], d[i + 7]);
    }
}
