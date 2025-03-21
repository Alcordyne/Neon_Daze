package tage.shapes;

import tage.*;
import tage.shapes.*;

public class ManualOctahedron extends ManualObject {
    float[] vertices = {
            0.0f,  1.0f,  0.0f,  -1.0f,  0.0f,  1.0f,   1.0f,  0.0f,  1.0f,  // Top front face
            0.0f,  1.0f,  0.0f,   1.0f,  0.0f,  1.0f,   1.0f,  0.0f, -1.0f,  // Top right face
            0.0f,  1.0f,  0.0f,   1.0f,  0.0f, -1.0f,  -1.0f,  0.0f, -1.0f,  // Top back face
            0.0f,  1.0f,  0.0f,  -1.0f,  0.0f, -1.0f,  -1.0f,  0.0f,  1.0f,  // Top left face
            0.0f, -1.0f,  0.0f,  -1.0f,  0.0f,  1.0f,   1.0f,  0.0f,  1.0f,  // Bottom front face
            0.0f, -1.0f,  0.0f,   1.0f,  0.0f,  1.0f,   1.0f,  0.0f, -1.0f,  // Bottom right face
            0.0f, -1.0f,  0.0f,   1.0f,  0.0f, -1.0f,  -1.0f,  0.0f, -1.0f,  // Bottom back face
            0.0f, -1.0f,  0.0f,  -1.0f,  0.0f, -1.0f,  -1.0f,  0.0f,  1.0f   // Bottom left face
    };

    float[] texCoords = {
            0.5f, 1.0f,  0.0f, 0.0f,  1.0f, 0.0f,
            0.5f, 1.0f,  0.0f, 0.0f,  1.0f, 0.0f,
            0.5f, 1.0f,  0.0f, 0.0f,  1.0f, 0.0f,
            0.5f, 1.0f,  0.0f, 0.0f,  1.0f, 0.0f,
            0.5f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f,
            0.5f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f,
            0.5f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f,
            0.5f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f
    };

    float[] normals = {
            0.0f,  1.0f,  0.0f,  0.0f,  1.0f,  0.0f,  0.0f,  1.0f,  0.0f,
            1.0f,  0.0f,  0.0f,  1.0f,  0.0f,  0.0f,  1.0f,  0.0f,  0.0f,
            0.0f, -1.0f,  0.0f,  0.0f, -1.0f,  0.0f,  0.0f, -1.0f,  0.0f,
            -1.0f,  0.0f,  0.0f, -1.0f,  0.0f,  0.0f, -1.0f,  0.0f,  0.0f
    };

    public ManualOctahedron() {
        setNumVertices(24);
        setVertices(vertices);
        setTexCoords(texCoords);
        setNormals(normals);
        setWindingOrderCCW(false);

        setMatAmb(Utils.silverAmbient());
        setMatDif(Utils.silverDiffuse());
        setMatSpe(Utils.silverSpecular());
        setMatShi(Utils.silverShininess());
    }
}
