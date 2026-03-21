package com.tradingplatform.listing.repository;

import com.tradingplatform.listing.entity.Category;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("findByParentIsNull returns empty list when parent is null (root categories)")
    void findByParentIsNull_nullParent_returnsEmptyList() {
        // Arrange - Create root categories (no parent)
        Category electronics = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices")
                .displayOrder(1)
                .build();

        Category phones = Category.builder()
                .name("Phones & Tablets")
                .slug("phones-tablets")
                .description("Phones and tablet devices")
                .displayOrder(2)
                .build();

        categoryRepository.save(electronics);
        categoryRepository.save(phones);

        // Act - Find categories where parent is null (root categories)
        List<Category> rootCategories = categoryRepository.findByParentIsNull();

        // Assert
        assertNotNull(rootCategories);
        assertEquals(2, rootCategories.size());
        assertTrue(rootCategories.stream().allMatch(c -> c.getParent() == null));
    }

    @Test
    @DisplayName("findBySlug returns Optional with category when slug exists")
    void findBySlug_existingSlug_returnsOptionalWithCategory() {
        // Arrange
        Category electronics = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices")
                .displayOrder(1)
                .build();
        categoryRepository.save(electronics);

        // Act
        Optional<Category> result = categoryRepository.findBySlug("electronics");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Electronics", result.get().getName());
        assertEquals("electronics", result.get().getSlug());
    }

    @Test
    @DisplayName("findBySlug returns empty Optional when slug does not exist")
    void findBySlug_nonExistingSlug_returnsEmptyOptional() {
        // Act
        Optional<Category> result = categoryRepository.findBySlug("non-existent-slug");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("isLeaf returns true when category has no children")
    void isLeaf_noChildren_returnsTrue() {
        // Arrange
        Category phones = Category.builder()
                .name("Smartphones")
                .slug("smartphones")
                .description("Smartphone devices")
                .displayOrder(1)
                .build();
        categoryRepository.save(phones);

        // Act & Assert
        assertTrue(phones.isLeaf());
    }

    @Test
    @DisplayName("isLeaf returns false when category has children")
    void isLeaf_hasChildren_returnsFalse() {
        // Arrange
        Category electronics = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices")
                .displayOrder(1)
                .build();
        categoryRepository.save(electronics);

        Category phones = Category.builder()
                .name("Phones & Tablets")
                .slug("phones-tablets")
                .description("Phones and tablet devices")
                .parent(electronics)
                .displayOrder(1)
                .build();
        categoryRepository.save(phones);

        // Flush and clear persistence context to simulate fresh load
        entityManager.flush();
        entityManager.clear();

        // Act - Re-load electronics and access children to trigger lazy load
        Category savedElectronics = categoryRepository.findById(electronics.getId()).orElseThrow();
        // Force initialization of children collection by accessing its size
        int childCount = savedElectronics.getChildren().size();

        // Assert - Electronics has children
        assertEquals(1, childCount);
        assertFalse(savedElectronics.isLeaf());
    }

    @Test
    @DisplayName("getDepth returns 0 for root category")
    void getDepth_rootCategory_returnsZero() {
        // Arrange
        Category electronics = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices")
                .displayOrder(1)
                .build();
        categoryRepository.save(electronics);

        // Act & Assert
        assertEquals(0, electronics.getDepth());
    }

    @Test
    @DisplayName("getDepth returns correct depth for nested categories")
    void getDepth_nestedCategory_returnsCorrectDepth() {
        // Arrange
        Category electronics = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices")
                .displayOrder(1)
                .build();
        categoryRepository.save(electronics);

        Category phones = Category.builder()
                .name("Phones & Tablets")
                .slug("phones-tablets")
                .description("Phones and tablet devices")
                .parent(electronics)
                .displayOrder(1)
                .build();
        categoryRepository.save(phones);

        Category smartphones = Category.builder()
                .name("Smartphones")
                .slug("smartphones")
                .description("Smartphone devices")
                .parent(phones)
                .displayOrder(1)
                .build();
        categoryRepository.save(smartphones);

        // Act & Assert
        assertEquals(1, phones.getDepth());
        assertEquals(2, smartphones.getDepth());
    }

    @Test
    @DisplayName("findByParentId returns children of a category")
    void findByParentId_existingParent_returnsChildren() {
        // Arrange
        Category electronics = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices")
                .displayOrder(1)
                .build();
        categoryRepository.save(electronics);

        Category phones = Category.builder()
                .name("Phones & Tablets")
                .slug("phones-tablets")
                .description("Phones and tablet devices")
                .parent(electronics)
                .displayOrder(1)
                .build();

        Category computers = Category.builder()
                .name("Computers & Laptops")
                .slug("computers-laptops")
                .description("Computers and laptop devices")
                .parent(electronics)
                .displayOrder(2)
                .build();

        categoryRepository.save(phones);
        categoryRepository.save(computers);

        // Act
        List<Category> children = categoryRepository.findByParentId(electronics.getId());

        // Assert
        assertEquals(2, children.size());
        assertTrue(children.stream().anyMatch(c -> c.getSlug().equals("phones-tablets")));
        assertTrue(children.stream().anyMatch(c -> c.getSlug().equals("computers-laptops")));
    }
}