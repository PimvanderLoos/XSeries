package com.cryptomorin.xseries;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

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

            try
            {
                XMaterial.matchXMaterial(material);
            }
            catch (IllegalArgumentException e)
            {
                unmatched.add(material);
            }
        }

        if (!unmatched.isEmpty())
        {
            StringBuilder sb = new StringBuilder("Unmatched Materials:\n");
            unmatched.stream().map(Enum::name).sorted().forEach(name -> sb.append(name).append(",\n"));
            fail(sb.toString());
        }
    }

    @ParameterizedTest
    @CsvSource(
        {
            "4598-Spigot-56165ca-d74c5d8 (MC: 1.21.11 Unobfuscated),1,21",
            "4616-Spigot-4a90bec-48244d7 (MC: 26.1.1),26,1"
        }
    )
    void parseServerMajorVersion_should(String input, int expectedMajor, int expectedMinor)
    {
        final XMaterial.Version version = XMaterial.parseServerVersion(input);

        assertEquals(expectedMajor, version.major(), "Unexpected major version for input: " + input);
        assertEquals(expectedMinor, version.minor(), "Unexpected minor version for input: " + input);
    }
}
