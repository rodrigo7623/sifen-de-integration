import { Search } from "lucide-react";
import { useMemo, useRef, useState } from "react";
import { TASA_IVA_LABEL, type Producto } from "../../api/types";

/** Minúsculas y sin tildes, para que "tornillo" encuentre "Tornillo" y "amoladora" encuentre "Amoládora". */
export function normalizar(texto: string): string {
  return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}

/** Coincide si TODAS las palabras buscadas aparecen (en cualquier orden) en la descripción o el código. */
export function coincide(producto: Producto, busqueda: string): boolean {
  const texto = normalizar(`${producto.descripcion} ${producto.codigo}`);
  return normalizar(busqueda)
    .split(/\s+/)
    .filter(Boolean)
    .every((palabra) => texto.includes(palabra));
}

const MAX_RESULTADOS = 50;

/**
 * Buscador del catálogo para agregar ítems: en vez de un desplegable con todos los productos (que se
 * estiraba con el catálogo), se escriben palabras de la descripción y se elige de una lista acotada.
 */
export function BuscadorProducto({
  productos,
  onElegir,
}: {
  productos: Producto[];
  onElegir: (producto: Producto) => void;
}) {
  const [busqueda, setBusqueda] = useState("");
  const [abierto, setAbierto] = useState(false);
  const [resaltado, setResaltado] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const resultados = useMemo(
    () => (busqueda.trim() ? productos.filter((p) => coincide(p, busqueda)) : productos),
    [productos, busqueda],
  );
  const visibles = resultados.slice(0, MAX_RESULTADOS);

  function elegir(producto: Producto) {
    onElegir(producto);
    setBusqueda("");
    setResaltado(0);
    setAbierto(false);
    inputRef.current?.blur();
  }

  return (
    <div className="relative w-full max-w-lg">
      <Search size={16} className="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 text-texto-suave" />
      <input
        ref={inputRef}
        className="pl-8"
        placeholder="Buscar producto del catálogo por palabras de la descripción o el código…"
        value={busqueda}
        onChange={(e) => {
          setBusqueda(e.target.value);
          setResaltado(0);
          setAbierto(true);
        }}
        onFocus={() => setAbierto(true)}
        onBlur={() => setTimeout(() => setAbierto(false), 150)}
        onKeyDown={(e) => {
          if (e.key === "ArrowDown") {
            e.preventDefault();
            setAbierto(true);
            setResaltado((i) => Math.min(i + 1, visibles.length - 1));
          } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setResaltado((i) => Math.max(i - 1, 0));
          } else if (e.key === "Enter") {
            e.preventDefault();
            if (abierto && visibles[resaltado]) elegir(visibles[resaltado]);
          } else if (e.key === "Escape") {
            setAbierto(false);
          }
        }}
      />
      {abierto && (
        <ul className="absolute z-20 mt-1 max-h-72 w-full overflow-y-auto rounded-md border border-borde bg-white py-1 text-sm shadow-modal">
          {visibles.length === 0 && <li className="px-3 py-2 text-texto-suave">Ningún producto coincide con “{busqueda}”.</li>}
          {visibles.map((p, i) => (
            <li
              key={p.codigo}
              onMouseDown={(e) => {
                e.preventDefault();
                elegir(p);
              }}
              onMouseEnter={() => setResaltado(i)}
              className={`flex cursor-pointer items-baseline justify-between gap-3 px-3 py-1.5 ${i === resaltado ? "bg-azul-claro" : ""}`}
            >
              <span>
                {p.descripcion} <span className="text-xs text-texto-suave">({p.codigo})</span>
              </span>
              <span className="whitespace-nowrap text-xs text-texto-suave">
                Gs. {p.precioBase.toLocaleString("es-PY")} · {TASA_IVA_LABEL[p.tasaIva]}
              </span>
            </li>
          ))}
          {resultados.length > MAX_RESULTADOS && (
            <li className="px-3 py-1.5 text-xs text-texto-suave">
              Se muestran {MAX_RESULTADOS} de {resultados.length}: escribí más palabras para acotar.
            </li>
          )}
        </ul>
      )}
    </div>
  );
}
