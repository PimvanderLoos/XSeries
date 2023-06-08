import com.cryptomorin.xseries.*;
import org.bukkit.Material;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public final class XSeriesTests {
    private XSeriesTests() {}

    private static void print(String str) {
        System.out.println(str);
    }

    public static void test() {
        print("\n\n\nTest begin...");

        print("Writing enum differences...");
        DifferenceHelper.versionDifference();

        print("Testing XMaterial...");
        assertPresent(XMaterial.matchXMaterial("AIR"));
        assertSame(XMaterial.matchXMaterial("CLAY_BRICK").get(), XMaterial.BRICK);
        assertMaterial("RED_BED", "RED_BED");
        assertMaterial("MELON", "MELON");
        assertMaterial("GREEN_CONCRETE_POWDER", "CONCRETE_POWDER:13");
        assertFalse(XMaterial.MAGENTA_TERRACOTTA.isOneOf(Arrays.asList("GREEN_TERRACOTTA", "BLACK_BED", "DIRT")));
        assertTrue(XMaterial.BLACK_CONCRETE.isOneOf(Arrays.asList("RED_CONCRETE", "CONCRETE:15", "CONCRETE:14")));
        for (Material material : Material.values()) if (!material.name().startsWith("LEGACY")) XMaterial.matchXMaterial(material);

        print("Testing reflection...");
        print("Version pack: " + ReflectionUtils.NMS_VERSION + " (" + ReflectionUtils.MINOR_NUMBER + ')');
        initializeReflection();

        print("\n\n\nTest end...");
    }

    private static void initializeReflection() {
        try {
            Class.forName("com.cryptomorin.xseries.messages.ActionBar");
            Class.forName("com.cryptomorin.xseries.messages.Titles");
            Class.forName("com.cryptomorin.xseries.SkullUtils");
            Class.forName("com.cryptomorin.xseries.NMSExtras");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void assertPresent(Optional<?> opt) {
        Assertions.assertTrue(opt.isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static void assertMaterial(String bukkitMaterial, String xMaterial) {
        Optional<XMaterial> mat = XMaterial.matchXMaterial(xMaterial);
        assertPresent(mat);
        Assertions.assertSame(Material.matchMaterial(bukkitMaterial), mat.get().parseMaterial());
    }
}
