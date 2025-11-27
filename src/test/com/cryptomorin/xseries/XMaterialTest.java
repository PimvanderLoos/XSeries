package com.cryptomorin.xseries;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class XMaterialTest
{
    @Test
    void findUnmatchedMaterials()
    {
        final List<Material> unmatched = new ArrayList<>();
        for (final var material : Material.values())
        {
            if (material.name().startsWith("LEGACY_"))
                continue;

            Optional<XMaterial> xMaterial = XMaterial.matchDefinedXMaterial(material.name(), XMaterial.UNKNOWN_DATA_VALUE);
            if (xMaterial.isEmpty())
            {
                unmatched.add(material);
            }
        }

        if (!unmatched.isEmpty())
        {
            StringBuilder sb = new StringBuilder("Unmatched Materials:\n");
            unmatched.stream().sorted().forEach(material -> sb.append(material.name()).append(",\n"));
            fail(sb.toString());
        }
    }
}
