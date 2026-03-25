package xyz.eclipseisoffline.eclipsescustomname;

import net.minecraft.resources.Identifier;
import xyz.eclipseisoffline.filefixutils.api.FileFixHelpers;
import xyz.eclipseisoffline.filefixutils.api.FileFixInitializer;

public class CustomNameFileFixes implements FileFixInitializer {

    @Override
    public void onFileFixPopulate() {
        FileFixHelpers.registerGlobalDataMoveFileFix("eclipsescustomname", Identifier.fromNamespaceAndPath("eclipsescustomname", "names"));
    }
}
