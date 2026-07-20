package com.Restaurante.Sistema.service;

import com.Restaurante.Sistema.entity.Insumo;
import com.Restaurante.Sistema.entity.PlatoBebida;
import com.Restaurante.Sistema.entity.PlatoInsumo;
import com.Restaurante.Sistema.repository.InsumoRepository;
import com.Restaurante.Sistema.repository.PlatoInsumoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Maneja el almacén: descuenta insumos cuando se vende un plato (según su receta)
 * y calcula cuántas porciones se pueden preparar con el stock disponible.
 */
@Service
@Transactional
public class InventarioService {

    /** Valor devuelto por {@link #porcionesDisponibles} cuando un plato no tiene receta (stock no controlado). */
    public static final int SIN_RECETA = -1;

    private final PlatoInsumoRepository recetaRepository;
    private final InsumoRepository insumoRepository;

    public InventarioService(PlatoInsumoRepository recetaRepository, InsumoRepository insumoRepository) {
        this.recetaRepository = recetaRepository;
        this.insumoRepository = insumoRepository;
    }

    /**
     * Porciones que se pueden preparar con el stock actual = mínimo, entre todos
     * los insumos de la receta, de floor(stockActual / cantidadReceta).
     * Devuelve {@link #SIN_RECETA} si el plato no tiene ingredientes definidos.
     */
    public int porcionesDisponibles(PlatoBebida plato) {
        List<PlatoInsumo> receta = recetaRepository.findByPlato(plato);
        if (receta.isEmpty()) return SIN_RECETA;

        int disponibles = Integer.MAX_VALUE;
        for (PlatoInsumo linea : receta) {
            Double cantReceta = linea.getCantidad();
            if (cantReceta == null || cantReceta <= 0) continue;
            double stock = linea.getInsumo().getStockActual() != null ? linea.getInsumo().getStockActual() : 0.0;
            int posibles = (int) Math.floor(stock / cantReceta);
            disponibles = Math.min(disponibles, posibles);
        }
        return disponibles == Integer.MAX_VALUE ? SIN_RECETA : disponibles;
    }

    /** ¿Hay stock suficiente para preparar {@code cantidad} porciones del plato? */
    public boolean hayStock(PlatoBebida plato, int cantidad) {
        int disp = porcionesDisponibles(plato);
        return disp == SIN_RECETA || disp >= cantidad;
    }

    /**
     * Descuenta del almacén los insumos necesarios para preparar {@code cantidad}
     * porciones del plato. Si el plato no tiene receta, no hace nada. El stock
     * nunca baja de 0.
     */
    public void descontarPorVenta(PlatoBebida plato, int cantidad) {
        if (cantidad <= 0) return;
        List<PlatoInsumo> receta = recetaRepository.findByPlato(plato);
        for (PlatoInsumo linea : receta) {
            Insumo insumo = linea.getInsumo();
            double stock = insumo.getStockActual() != null ? insumo.getStockActual() : 0.0;
            double consumo = (linea.getCantidad() != null ? linea.getCantidad() : 0.0) * cantidad;
            insumo.setStockActual(Math.max(0.0, stock - consumo));
            insumoRepository.save(insumo);
        }
    }
}
