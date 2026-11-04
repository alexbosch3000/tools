// SQL access to the item table.
package io.github.alexbosch3000.familyhub.catalog.internal;

import io.github.alexbosch3000.familyhub.catalog.Aisle;
import io.github.alexbosch3000.familyhub.catalog.CatalogService.ItemData;
import io.github.alexbosch3000.familyhub.catalog.Item;
import io.github.alexbosch3000.familyhub.catalog.ItemKind;
import io.github.alexbosch3000.familyhub.catalog.StoreType;
import io.github.alexbosch3000.familyhub.catalog.Unit;
import io.github.alexbosch3000.familyhub.shared.Db;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ItemRepository {

    private static final String SELECT = "SELECT id, name, kind, store_type, aisle, unit, unit_price_cents FROM item";

    private final JdbcClient jdbc;

    ItemRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Item> find(@Nullable ItemKind kind, @Nullable String query) {
        String like = query == null || query.isBlank() ? null : "%" + query.trim() + "%";
        return jdbc.sql(SELECT + " " + """
                 WHERE (:kind IS NULL OR kind = :kind) AND (:like IS NULL OR name LIKE :like)
                 ORDER BY name COLLATE NOCASE""")
                .param("kind", Db.text(kind)).param("like", like)
                .query(ItemRepository::map).list();
    }

    public Optional<Item> findById(long id) {
        return jdbc.sql(SELECT + " WHERE id = ?").param(id).query(ItemRepository::map).optional();
    }

    public Map<Long, Item> findByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql(SELECT + " WHERE id IN (:ids)").param("ids", ids).query(ItemRepository::map).list()
                .stream().collect(Collectors.toMap(Item::id, Function.identity()));
    }

    public boolean nameTaken(String name, @Nullable Long exceptId) {
        return jdbc.sql("SELECT COUNT(*) FROM item WHERE name = ? AND id IS NOT ?").params(name, exceptId)
                .query(Long.class).single() > 0;
    }

    public long insert(ItemData data) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO item (name, kind, store_type, aisle, unit, unit_price_cents)
                VALUES (?, ?, ?, ?, ?, ?)""")
                .params(data.name().trim(), data.kind().name(), data.storeType().name(), data.aisle().name(),
                        data.unit().name(), data.unitPriceCents())
                .update(keys);
        return Db.key(keys);
    }

    public void update(long id, ItemData data) {
        jdbc.sql("""
                UPDATE item SET name = ?, kind = ?, store_type = ?, aisle = ?, unit = ?, unit_price_cents = ?
                WHERE id = ?""")
                .params(data.name().trim(), data.kind().name(), data.storeType().name(), data.aisle().name(),
                        data.unit().name(), data.unitPriceCents(), id)
                .update();
    }

    public void delete(long id) {
        jdbc.sql("DELETE FROM item WHERE id = ?").param(id).update();
    }

    private static Item map(ResultSet rs, int row) throws SQLException {
        return new Item(rs.getLong("id"), rs.getString("name"), ItemKind.valueOf(rs.getString("kind")),
                StoreType.valueOf(rs.getString("store_type")), Aisle.valueOf(rs.getString("aisle")),
                Unit.valueOf(rs.getString("unit")), Db.nullableLong(rs, "unit_price_cents"));
    }
}
