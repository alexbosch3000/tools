// Shopping lists API.
package io.github.alexbosch3000.familyhub.shopping.internal;

import io.github.alexbosch3000.familyhub.catalog.Quantity;
import io.github.alexbosch3000.familyhub.catalog.StoreType;
import io.github.alexbosch3000.familyhub.catalog.Unit;
import io.github.alexbosch3000.familyhub.shopping.ShoppingList;
import io.github.alexbosch3000.familyhub.shopping.ShoppingListSummary;
import io.github.alexbosch3000.familyhub.shopping.ShoppingPayment;
import io.github.alexbosch3000.familyhub.shopping.ShoppingService;
import io.github.alexbosch3000.familyhub.shopping.ShoppingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;
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
@RequestMapping("/api/shopping/lists")
class ShoppingController {

    private final ShoppingService shopping;

    ShoppingController(ShoppingService shopping) {
        this.shopping = shopping;
    }

    record ShoppingListRequest(@NotBlank @Size(max = 80) String name, @Nullable LocalDate plannedFor) {
    }

    /** @param memberId the member the item is for (clothes, shoes); absent for the household */
    record LineRequest(@NotNull Long itemId, @Nullable Long memberId, @Positive double quantity, @NotNull Unit unit,
            @Nullable @Size(max = 120) String note) {
    }

    record CheckRequest(boolean checked) {
    }

    record PaymentRequest(@NotNull StoreType storeType, @PositiveOrZero long amountCents) {
    }

    /** @param skippedItemIds ingredients the family already has, left out of the list */
    record ListRecipeRequest(@NotNull Long recipeId, @Min(1) @Max(50) int servings,
            @Nullable @Size(max = 100) List<Long> skippedItemIds) {
    }

    /** @param payments what was paid in each kind of shop, each recorded in its budget category */
    record CompleteRequest(@NotEmpty @Size(max = 12) List<@Valid PaymentRequest> payments, @Nullable LocalDate paidOn,
            @Nullable Long accountId) {
    }

    @GetMapping
    List<ShoppingListSummary> list(@RequestParam(defaultValue = "OPEN") ShoppingStatus status) {
        return shopping.list(status);
    }

    @GetMapping("/{id}")
    ShoppingList get(@PathVariable long id) {
        return shopping.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShoppingList create(@Valid @RequestBody ShoppingListRequest request) {
        return shopping.create(request.name(), request.plannedFor());
    }

    @PutMapping("/{id}")
    ShoppingList update(@PathVariable long id, @Valid @RequestBody ShoppingListRequest request) {
        return shopping.update(id, request.name(), request.plannedFor());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        shopping.delete(id);
    }

    @PostMapping("/{id}/lines")
    ShoppingList addLine(@PathVariable long id, @Valid @RequestBody LineRequest request) {
        return shopping.addItem(id, request.itemId(), request.memberId(),
                new Quantity(request.quantity(), request.unit()), request.note());
    }

    /** A row is an item for a member, or for the household when memberId is absent. */
    @DeleteMapping("/{id}/items/{itemId}")
    ShoppingList removeRow(@PathVariable long id, @PathVariable long itemId,
            @RequestParam(required = false) @Nullable Long memberId) {
        return shopping.removeRow(id, itemId, memberId);
    }

    @PutMapping("/{id}/items/{itemId}/checked")
    ShoppingList check(@PathVariable long id, @PathVariable long itemId,
            @RequestParam(required = false) @Nullable Long memberId, @RequestBody CheckRequest request) {
        return shopping.setChecked(id, itemId, memberId, request.checked());
    }

    @PostMapping("/{id}/recipes")
    ShoppingList addRecipe(@PathVariable long id, @Valid @RequestBody ListRecipeRequest request) {
        return shopping.addRecipe(id, request.recipeId(), request.servings(),
                request.skippedItemIds() == null ? List.of() : request.skippedItemIds());
    }

    @DeleteMapping("/{id}/recipes/{entryId}")
    ShoppingList removeRecipe(@PathVariable long id, @PathVariable long entryId) {
        return shopping.removeRecipe(id, entryId);
    }

    @PostMapping("/{id}/complete")
    ShoppingList complete(@PathVariable long id, @Valid @RequestBody CompleteRequest request) {
        return shopping.complete(id, request.payments().stream()
                .map(p -> new ShoppingPayment(p.storeType(), p.amountCents())).toList(),
                request.paidOn(), request.accountId());
    }
}
