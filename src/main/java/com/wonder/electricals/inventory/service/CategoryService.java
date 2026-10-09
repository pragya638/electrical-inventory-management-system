package com.wonder.electricals.inventory.service;

import com.wonder.electricals.inventory.dto.CategoryRequest;
import com.wonder.electricals.inventory.dto.response.CategoryResponse;
import com.wonder.electricals.inventory.entity.Category;
import com.wonder.electricals.inventory.exception.DuplicateResourceException;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.CategoryRepository;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Category master data. Enforces the unique-name rule (business rule 9). */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DtoMapper mapper;

    public CategoryService(CategoryRepository categoryRepository, DtoMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    public List<CategoryResponse> findAll(String search) {
        List<Category> categories = categoryRepository.findAll();
        if (search != null && !search.isBlank()) {
            String needle = search.trim().toLowerCase();
            categories = categories.stream()
                    .filter(category -> category.getName().toLowerCase().contains(needle)
                            || (category.getDescription() != null
                                && category.getDescription().toLowerCase().contains(needle)))
                    .toList();
        }
        return mapper.toCategoryResponses(categories);
    }

    public CategoryResponse findById(Long id) {
        return mapper.toCategoryResponse(getEntity(id));
    }

    public List<CategoryResponse> findActive() {
        return mapper.toCategoryResponses(
                categoryRepository.findAll().stream().filter(Category::isActive).toList());
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw DuplicateResourceException.of("category name", name);
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(request.getDescription());
        category.setActive(request.getActive() == null || request.getActive());
        return mapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw DuplicateResourceException.of("category name", name);
        }
        category.setName(name);
        category.setDescription(request.getDescription());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        return mapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse changeStatus(Long id, boolean active) {
        Category category = getEntity(id);
        category.setActive(active);
        return mapper.toCategoryResponse(categoryRepository.save(category));
    }

    private Category getEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }
}
