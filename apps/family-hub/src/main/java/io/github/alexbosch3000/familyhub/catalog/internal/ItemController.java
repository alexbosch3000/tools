// Catalog API: list, create, update and delete purchasable items.
package io.github.alexbosch3000.familyhub.catalog.internal;

import io.github.alexbosch3000.familyhub.catalog.Aisle;
import io.github.alexbosch3000.familyhub.catalog.CatalogService;
import io.github.alexbosch3000.familyhub.catalog.CatalogService.ItemData;
import io.github.alexbosch3000.familyhub.catalog.Item;
import io.github.alexbosch3000.familyhub.catalog.ItemKind;
import io.github.alexbosch3000.familyhub.catalog.StoreType;
import io.github.alexbosch3000.familyhub.catalog.Unit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
class ItemController {

    private final CatalogService catalog;

    ItemController(CatalogService catalog) {
        this.catalog = catalog;
    }

    record ItemRequest(
            @NotBlank @Size(max = 80) String name,
            @NotNull ItemKind kind,
            @NotNull StoreType storeType,
            @NotNull Aisle aisle,
            @NotNull Unit unit,
            @PositiveOrZero Long unitPriceCents) {

        ItemData toData() {
            return new ItemData(name, kind, storeType, aisle, unit, unitPriceCents);
        }
    }

    @GetMapping
    List<Item> list(@RequestParam(required = false) ItemKind kind, @RequestParam(required = false) String q) {
        return catalog.list(kind, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Item create(@Valid @RequestBody ItemRequest request) {
        return catalog.create(request.toData());
    }

    @PutMapping("/{id}")
    Item update(@PathVariable long id, @Valid @RequestBody ItemRequest request) {
        return catalog.update(id, request.toData());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        catalog.delete(id);
    }
}
