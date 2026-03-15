package xyz.eclipseisoffline.eclipsescustomname.filefix;

import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.operations.FileFixOperations;

public class CustomNameStorageFileFix extends FileFix {

    public CustomNameStorageFileFix(Schema schema) {
        super(schema);
    }

    @Override
    public void makeFixer() {
        addFileFixOperation(FileFixOperations.move("data/eclipsescustomname.dat", "data/eclipsescustomname/names.dat"));
    }
}
