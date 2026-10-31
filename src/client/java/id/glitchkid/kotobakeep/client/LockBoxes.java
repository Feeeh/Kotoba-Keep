package id.glitchkid.kotobakeep.client;

import id.glitchkid.kotobakeep.KotobaKeep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Draws a see-through box with a red 🚫 on each side around every mob drop that is still locked. */
final class LockBoxes {
    private static final double HALF_WIDTH = 0.25, HEIGHT = 0.5;
    private static final float STROKE_WIDTH = 2f, SIGN_WIDTH = 2.5f;
    private static final int FILL_ALPHA = 0x33000000, SIGN_COLOR = 0xFFE53935, SIGN_SEGMENTS = 16;
    private static final double SIGN_RADIUS = 0.17, SIGN_LIFT = 0.004; // lift keeps the sign off the face it sits on
    private static final Vec3 UP = new Vec3(0, 1, 0), NORTH = new Vec3(0, 0, -1);
    private static final Vec3[] SIDES = {new Vec3(1, 0, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, 1), NORTH};
    private LockBoxes() {}

    static void draw(LevelRenderer renderer, int[] ids) {
        var mc = Minecraft.getInstance();
        if (ids.length == 0 || mc.level == null) return;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 camera = mc.gameRenderer.mainCamera().position();
        int stroke = ChallengeHud.ACCENT;
        var style = GizmoStyle.strokeAndFill(stroke, STROKE_WIDTH, stroke & 0x00FFFFFF | FILL_ALPHA);
        try (var _ = renderer.collectPerFrameRenderThreadGizmos()) {
            for (int id : ids) {
                if (!(mc.level.getEntity(id) instanceof ItemEntity item)) continue;
                AABB box = box(item, partial);
                Gizmos.cuboid(box, style);
                drawSigns(box, camera);
            }
        }
    }
    /** Same bob as the vanilla item renderer, so the box rises and falls with the item inside it. */
    private static AABB box(ItemEntity item, float partial) {
        var pos = item.getPosition(partial);
        double bob = Mth.sin((item.getAge() + partial) / 10.0 + item.bobOffs) * 0.1 + 0.1;
        return new AABB(pos.x - HALF_WIDTH, pos.y + bob, pos.z - HALF_WIDTH, pos.x + HALF_WIDTH, pos.y + bob + HEIGHT, pos.z + HALF_WIDTH);
    }
    /** Only faces turned towards the camera: a back sign seen through the box would read as an X. */
    private static void drawSigns(AABB box, Vec3 camera) {
        Vec3 centre = box.getCenter();
        for (Vec3 normal : SIDES) {
            Vec3 face = centre.add(normal.scale(HALF_WIDTH + SIGN_LIFT));
            // "Right" as seen by someone looking at this face, so every slash runs top-left to bottom-right.
            if (camera.subtract(face).dot(normal) > 0) drawSign(face, normal.reverse().cross(UP), UP);
        }
        Vec3 top = centre.add(UP.scale(HEIGHT / 2 + SIGN_LIFT));
        if (camera.y > top.y) drawSign(top, new Vec3(1, 0, 0), NORTH);
    }
    private static void drawSign(Vec3 centre, Vec3 right, Vec3 up) {
        Vec3 previous = null;
        for (int i = 0; i <= SIGN_SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / SIGN_SEGMENTS;
            Vec3 point = centre.add(right.scale(Math.cos(angle) * SIGN_RADIUS)).add(up.scale(Math.sin(angle) * SIGN_RADIUS));
            if (previous != null) Gizmos.line(previous, point, SIGN_COLOR, SIGN_WIDTH);
            previous = point;
        }
        double d = SIGN_RADIUS / Math.sqrt(2);
        Gizmos.line(centre.add(right.scale(-d)).add(up.scale(d)), centre.add(right.scale(d)).add(up.scale(-d)), SIGN_COLOR, SIGN_WIDTH);
    }
}
