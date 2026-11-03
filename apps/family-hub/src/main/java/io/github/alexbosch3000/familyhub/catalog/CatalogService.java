// Public API of the catalog module: purchasable items and ingredients.
package io.github.alexbosch3000.familyhub.catalog;

import io.github.alexbosch3000.familyhub.catalog.internal.ItemRepository;
import io.github.alexbosch3000.familyhub.shared.ConflictException;
import io.github.alexbosch3000.familyhub.shared.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final ItemRepository repository;

    CatalogService(ItemRepository repository) {
        this.repository = repository;
    }

    public record ItemData(String name, ItemKind kind, StoreType storeType, Aisle aisle, Unit unit,
            Long unitPriceCents) {
    }

    public List<Item> list(ItemKind kind, String query) {
        return repository.find(kind, query);
    }

    public Item get(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Article introuvable."));
    }

    /** Items by id; unknown ids are absent from the map. */
    public Map<Long, Item> getAll(Collection<Long> ids) {
        return repository.findByIds(ids);
    }

    @Transactional
    public Item create(ItemData data) {
        ensureNameFree(data.name(), null);
        return get(repository.insert(data));
    }

    @Transactional
    public Item update(long id, ItemData data) {
        get(id);
        ensureNameFree(data.name(), id);
        repository.update(id, data);
        return get(id);
    }

    /** Fails with a conflict when the item is still used by a recipe or a shopping list. */
    @Transactional
    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    private void ensureNameFree(String name, Long exceptId) {
        if (repository.nameTaken(name.trim(), exceptId)) {
            throw new ConflictException("Un article porte déjà ce nom.");
        }
    }
}
