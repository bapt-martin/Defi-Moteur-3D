package engines.graphicEngine.io;

import engines.graphicEngine.math.geometry.Mesh;
import engines.graphicEngine.math.geometry.Triangle;
import engines.graphicEngine.math.geometry.Vertex2D;
import engines.graphicEngine.math.geometry.Vertex3D;
import engines.graphicEngine.math.tools.Vector3D;

import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class ObjLoader {

    public static Mesh loadMesh(Path objPath) {
        Path fileName = objPath.getFileName();
        String cachePathStr = objPath + ".cache";
        Path cachePath = Paths.get(cachePathStr);

        if (Files.exists(cachePath)) {
//                System.out.println("binary cache loading for : " + fileName);
            Mesh cachedMesh = loadFromCustomCache(cachePath);

            if (cachedMesh != null) {
                return cachedMesh;
            }

            System.err.println("binary cache loading error, classic OBJ reading for : " + fileName);
        }

        System.out.println("first loading, OBJ parsing for : " + fileName);
        Mesh mesh = readObjFile(objPath);

        System.out.println("binary cache saving for : " + fileName);
        saveToCustomCache(mesh, cachePath);

        return mesh;
    }

    public static Mesh loadFromCustomCache(Path filepath) {
        File file = filepath.toFile();
        if (!file.exists()) return null;

        Mesh mesh = new Mesh();
        List<Triangle> meshTriangles = mesh.getMeshTriangle();

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataInputStream dis = new DataInputStream(bis)) {

            int nbTri = dis.readInt();

            for (int i = 0; i < nbTri; i++) {
                Vertex3D[] triVerts = new Vertex3D[3];
                Vertex2D[] textVerts = new Vertex2D[3];
                Vector3D[] triNormals = new Vector3D[3];

                for (int j = 0; j < 3; j++) {
                    triVerts[j]   = new Vertex3D(dis.readDouble(), dis.readDouble(), dis.readDouble());
                    textVerts[j]  = new Vertex2D(dis.readDouble(), dis.readDouble());
                    triNormals[j] = new Vector3D(dis.readDouble(), dis.readDouble(), dis.readDouble());
                }

                Color color = new Color(dis.readInt());

                meshTriangles.add(Triangle.createShallowTriangle(triVerts, textVerts, triNormals, color));
            }

//            System.out.println("data loaded");

            return mesh;

        } catch (IOException e) {
            System.err.println("failed loading: " + e.getMessage());
            return null;
        }
    }

    public static void saveToCustomCache(Mesh mesh, Path cachePath) {
        List<Triangle> triangles = mesh.getMeshTriangle();

        try (FileOutputStream fos = new FileOutputStream(cachePath.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(bos)) {


            dos.writeInt(triangles.size());

            for (Triangle tri : triangles) {
                Vertex3D[] triVerts   = tri.getVertices();
                Vertex2D[] textVerts  = tri.getTextVertices();
                Vector3D[] triNormals = tri.getNormalsVertices();
                Color color           = tri.getColor();

                for (int i = 0; i < 3; i++) {
                    dos.writeDouble(triVerts[i].x);
                    dos.writeDouble(triVerts[i].y);
                    dos.writeDouble(triVerts[i].z);

                    dos.writeDouble(textVerts[i].u);
                    dos.writeDouble(textVerts[i].v);

                    dos.writeDouble(triNormals[i].x);
                    dos.writeDouble(triNormals[i].y);
                    dos.writeDouble(triNormals[i].z);
                }

                dos.writeInt(color.getRGB());
            }

            System.out.println("success saving" + triangles.size() + " triangles");

        } catch (IOException e) {
            System.err.println("saving failure : " + e.getMessage());
        }
    }

    public static Mesh readObjFile(Path path) {
        Mesh mesh = new Mesh();

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            List<Vertex3D> vertices = new ArrayList<>();
            List<Vector3D> sharedNormals = new ArrayList<>();
            List<Vertex2D> textVertices = new ArrayList<>();

            List<Triangle> meshTriangles = mesh.getMeshTriangle();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] tokens = line.split("\\s+");
                String prefix = tokens[0];

                switch (prefix) {
                    case "v" -> {
                        float x = Float.parseFloat(tokens[1]);
                        float y = Float.parseFloat(tokens[2]);
                        float z = Float.parseFloat(tokens[3]);
                        vertices.add(new Vertex3D(x, y, z));
                        sharedNormals.add(new Vector3D(0, 0, 0));
                    }
                    case "vt" -> {
                        float u = Float.parseFloat(tokens[1]);
                        float v = Float.parseFloat(tokens[2]);
                        textVertices.add(new Vertex2D(u, v));
                    }
                    case "f" -> {
                        String[] vertex1 = tokens[1].split("/");
                        String[] vertex2 = tokens[2].split("/");
                        String[] vertex3 = tokens[3].split("/");

                        int v1 = Integer.parseInt(vertex1[0]) - 1;
                        int v2 = Integer.parseInt(vertex2[0]) - 1;
                        int v3 = Integer.parseInt(vertex3[0]) - 1;

                        Vertex3D[] triVerts = new Vertex3D[] {
                                vertices.get(v1), vertices.get(v2), vertices.get(v3)
                        };

                        Vector3D[] triNormals = new Vector3D[] {
                                sharedNormals.get(v1), sharedNormals.get(v2), sharedNormals.get(v3)
                        };

                        Vertex2D[] triUVs = new Vertex2D[3];

                        if (vertex1.length > 1 && !vertex1[1].isEmpty()) {
                            int vt1 = Integer.parseInt(vertex1[1]) - 1;
                            int vt2 = Integer.parseInt(vertex2[1]) - 1;
                            int vt3 = Integer.parseInt(vertex3[1]) - 1;

                            triUVs[0] = textVertices.get(vt1);
                            triUVs[1] = textVertices.get(vt2);
                            triUVs[2] = textVertices.get(vt3);
                        } else {
                            triUVs[0] = new Vertex2D(0, 0);
                            triUVs[1] = new Vertex2D(0, 0);
                            triUVs[2] = new Vertex2D(0, 0);
                        }

                        meshTriangles.add(Triangle.createShallowTriangle(triVerts, triUVs, triNormals, Color.WHITE));
                    }
                }
            }

            mesh.createTriangleNormal();

            return mesh;

        } catch (IOException e) {
            System.err.println("Error while reading file: " + path.toString());
            e.printStackTrace();
            return new Mesh();
        }
    }
}

//    public static void main(String[] args) {
//
//    }