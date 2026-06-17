    package engines.graphicEngine.io;

    import engines.graphicEngine.math.geometry.Mesh;
    import engines.graphicEngine.math.geometry.Triangle;
    import engines.graphicEngine.math.geometry.Vertex2D;
    import engines.graphicEngine.math.geometry.Vertex3D;
    import engines.graphicEngine.math.tools.Vector3D;

    import java.awt.Color;
    import java.io.BufferedReader;
    import java.io.IOException;
    import java.nio.channels.FileChannel;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.StandardOpenOption;
    import java.util.ArrayList;
    import java.util.List;

    public abstract class ObjLoader {

        public static boolean isFileOpen(Path path) {
            try (var channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
                return false;
            } catch (IOException e) {
                return true;
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

                        meshTriangles.add(new Triangle(triVerts, triUVs, triNormals, Color.WHITE));
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