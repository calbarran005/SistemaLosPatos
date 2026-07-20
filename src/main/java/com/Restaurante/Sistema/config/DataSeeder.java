package com.Restaurante.Sistema.config;

import com.Restaurante.Sistema.entity.*;
import com.Restaurante.Sistema.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Carga datos de ejemplo la primera vez que arranca la app (idempotente: cada
 * bloque solo se ejecuta si su tabla está vacía, así no duplica lo que ya exista).
 * Insumos → Proveedores → Categorías → Platos con receta.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoriaMenuRepository categoriaRepo;
    private final InsumoRepository insumoRepo;
    private final ProveedorRepository proveedorRepo;
    private final PlatoBebidaRepository platoRepo;
    private final PlatoInsumoRepository recetaRepo;

    public DataSeeder(CategoriaMenuRepository categoriaRepo,
                      InsumoRepository insumoRepo,
                      ProveedorRepository proveedorRepo,
                      PlatoBebidaRepository platoRepo,
                      PlatoInsumoRepository recetaRepo) {
        this.categoriaRepo = categoriaRepo;
        this.insumoRepo = insumoRepo;
        this.proveedorRepo = proveedorRepo;
        this.platoRepo = platoRepo;
        this.recetaRepo = recetaRepo;
    }

    @Override
    public void run(String... args) {
        seedInsumos();
        seedProveedores();
        seedCategorias();
        seedPlatosConReceta();
    }

    // ── Almacén de insumos ──────────────────────────────────────────────────
    private void seedInsumos() {
        if (insumoRepo.count() > 0) return;
        insumoRepo.save(insumo("Arroz", "GRAMOS", 5000, 1000));
        insumoRepo.save(insumo("Pato", "UNIDAD", 6, 2));
        insumoRepo.save(insumo("Pollo", "GRAMOS", 7000, 1500));
        insumoRepo.save(insumo("Papa", "GRAMOS", 8000, 1500));
        insumoRepo.save(insumo("Cebolla", "GRAMOS", 3000, 500));
        insumoRepo.save(insumo("Ají amarillo", "GRAMOS", 1500, 300));
        insumoRepo.save(insumo("Aceite", "MILILITROS", 4000, 800));
        insumoRepo.save(insumo("Culantro", "GRAMOS", 800, 200));
        insumoRepo.save(insumo("Limón", "UNIDAD", 50, 15));
        insumoRepo.save(insumo("Gaseosa personal", "UNIDAD", 40, 10));
        insumoRepo.save(insumo("Cerveza", "UNIDAD", 30, 8));
    }

    private Insumo insumo(String nombre, String unidad, double stock, double minimo) {
        Insumo i = new Insumo();
        i.setNombre(nombre);
        i.setUnidadMedida(unidad);
        i.setStockActual(stock);
        i.setStockMinimo(minimo);
        return i;
    }

    // ── Proveedores ─────────────────────────────────────────────────────────
    private void seedProveedores() {
        if (proveedorRepo.count() > 0) return;
        proveedorRepo.save(proveedor("Distribuidora de Alimentos del Norte S.A.C.", "20481234567",
                "944112233", "ventas@alimentosnorte.pe", "Av. Los Almendros 456, Trujillo"));
        proveedorRepo.save(proveedor("Avícola Los Andes E.I.R.L.", "20551239874",
                "955667788", "contacto@avicolalosandes.pe", "Jr. Comercio 120, Lima"));
        proveedorRepo.save(proveedor("Bebidas y Licores Perú S.A.", "20609871234",
                "966554433", "pedidos@bebidasperu.com", "Av. Industrial 789, Lima"));
    }

    private Proveedor proveedor(String razon, String ruc, String tel, String email, String dir) {
        Proveedor p = new Proveedor();
        p.setRazonSocial(razon);
        p.setRuc(ruc);
        p.setTelefono(tel);
        p.setEmail(email);
        p.setDireccion(dir);
        return p;
    }

    // ── Categorías del menú ─────────────────────────────────────────────────
    private void seedCategorias() {
        if (categoriaRepo.count() > 0) return;
        categoriaRepo.save(categoria("Entradas", "Piqueos y entradas frías o calientes"));
        categoriaRepo.save(categoria("Platos de Fondo", "Platos principales de la casa"));
        categoriaRepo.save(categoria("Bebidas", "Gaseosas, cervezas y refrescos"));
        categoriaRepo.save(categoria("Postres", "Dulces y postres tradicionales"));
    }

    private CategoriaMenu categoria(String nombre, String desc) {
        CategoriaMenu c = new CategoriaMenu();
        c.setNombre(nombre);
        c.setDescripcion(desc);
        return c;
    }

    // ── Platos de la carta con su receta (ficha técnica) ─────────────────────
    private void seedPlatosConReceta() {
        // Solo si aún no existe ninguna receta (no duplica en reinicios ni toca
        // los platos que el usuario ya haya creado; agrega platos-demo con su receta).
        if (recetaRepo.count() > 0) return;

        Map<String, Insumo> ins = new HashMap<>();
        insumoRepo.findAll().forEach(i -> ins.put(i.getNombre().toLowerCase(), i));

        CategoriaMenu entradas = getOrCreateCategoria("Entradas");
        CategoriaMenu fondos = getOrCreateCategoria("Platos de Fondo");
        CategoriaMenu bebidas = getOrCreateCategoria("Bebidas");

        // Arroz con Pato
        PlatoBebida arrozPato = plato("Arroz con Pato", "Arroz verde con pato tierno, cebolla y culantro.", 32.0, fondos,
                "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=600");
        receta(arrozPato, ins, "arroz", 220);
        receta(arrozPato, ins, "pato", 0.25);
        receta(arrozPato, ins, "cebolla", 40);
        receta(arrozPato, ins, "aceite", 30);
        receta(arrozPato, ins, "culantro", 15);
        receta(arrozPato, ins, "ají amarillo", 20);

        // Ají de Gallina
        PlatoBebida ajiGallina = plato("Ají de Gallina", "Cremoso ají de gallina con papa y arroz blanco.", 28.0, fondos,
                "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=600");
        receta(ajiGallina, ins, "pollo", 200);
        receta(ajiGallina, ins, "aceite", 25);
        receta(ajiGallina, ins, "ají amarillo", 30);
        receta(ajiGallina, ins, "cebolla", 30);
        receta(ajiGallina, ins, "arroz", 150);

        // Papa a la Huancaína
        PlatoBebida huancaina = plato("Papa a la Huancaína", "Papas sancochadas bañadas en crema de ají.", 14.0, entradas,
                "https://images.unsplash.com/photo-1608835291093-394b0c943a75?w=600");
        receta(huancaina, ins, "papa", 250);
        receta(huancaina, ins, "ají amarillo", 25);
        receta(huancaina, ins, "aceite", 20);

        // Bebidas
        PlatoBebida gaseosa = plato("Gaseosa Personal", "Gaseosa personal 500ml bien helada.", 5.0, bebidas, "");
        receta(gaseosa, ins, "gaseosa personal", 1);

        PlatoBebida cerveza = plato("Cerveza", "Cerveza nacional 355ml.", 10.0, bebidas, "");
        receta(cerveza, ins, "cerveza", 1);
    }

    private PlatoBebida plato(String nombre, String desc, double precio, CategoriaMenu cat, String img) {
        PlatoBebida p = new PlatoBebida();
        p.setNombre(nombre);
        p.setDescripcion(desc);
        p.setPrecioVenta(precio);
        p.setCategoria(cat);
        p.setDisponible(true);
        p.setImagenUrl(img);
        return platoRepo.save(p);
    }

    private void receta(PlatoBebida plato, Map<String, Insumo> ins, String insumoNombre, double cantidad) {
        Insumo insumo = ins.get(insumoNombre.toLowerCase());
        if (insumo == null) return;
        PlatoInsumo pi = new PlatoInsumo();
        pi.setPlato(plato);
        pi.setInsumo(insumo);
        pi.setCantidad(cantidad);
        recetaRepo.save(pi);
    }

    private CategoriaMenu getOrCreateCategoria(String nombre) {
        return categoriaRepo.findAll().stream()
                .filter(c -> nombre.equalsIgnoreCase(c.getNombre()))
                .findFirst()
                .orElseGet(() -> categoriaRepo.save(categoria(nombre, "")));
    }
}
