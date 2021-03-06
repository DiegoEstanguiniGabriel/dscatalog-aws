package com.devsuperior.dscatalog.resources;

import com.devsuperior.dscatalog.dto.ProductDTO;
import com.devsuperior.dscatalog.services.ProductService;
import com.devsuperior.dscatalog.services.exceptions.DatabaseException;
import com.devsuperior.dscatalog.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dscatalog.tests.Factory;
import com.devsuperior.dscatalog.tests.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductResourceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenUtil tokenUtil;

    @MockBean
    private ProductService service;
    private ProductDTO productDTO;
    private Long existingId;
    private Long nonExistingId;
    private Long dependentId;

    private String username;
    private String password;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception{

        username = "maria@gmail.com";
        password = "123456";

        existingId = 1L;
        nonExistingId = 2L;
        dependentId = 3L;
        productDTO = Factory.createProductDto();

        when(service.findAll(any(), any(), any())).thenReturn(new PageImpl<>(List.of(productDTO)));

        when(service.findById(existingId)).thenReturn(productDTO);
        when(service.findById(nonExistingId)).thenThrow(ResourceNotFoundException.class);

        when(service.update(eq(existingId), any())).thenReturn(productDTO);
        when(service.update(eq(nonExistingId), any())).thenThrow(ResourceNotFoundException.class);

        when(service.insert(any())).thenReturn(productDTO);

        doNothing().when(service).delete(existingId);
        doThrow(ResourceNotFoundException.class).when(service).delete(nonExistingId);
        doThrow(DatabaseException.class).when(service).delete(dependentId);
    }

    @Test
    public void deleteShouldReturnNotFoundWhenNonExistingId() throws Exception{
        String accessToken = tokenUtil.obtainAccessToken(mockMvc, username, password);

        mockMvc.perform(delete("/products/{id}", nonExistingId).header("Authorization", "Bearer " + accessToken)).andExpect(status().isNotFound());
    }

    @Test
    public void deleteShouldReturnNoContentWhenExistingId() throws Exception{
        String accessToken = tokenUtil.obtainAccessToken(mockMvc, username, password);

        mockMvc.perform(delete("/products/{id}", existingId).header("Authorization", "Bearer " + accessToken)).andExpect(status().isNoContent());
    }

    @Test
    public void insertShouldReturnProductDto() throws Exception{
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        String accessToken = tokenUtil.obtainAccessToken(mockMvc, username, password);

        mockMvc.perform(post("/products/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(jsonBody)
                .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    public void updateShouldReturnProductDtoWhenExistingId() throws Exception{
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        String accessToken = tokenUtil.obtainAccessToken(mockMvc, username, password);

        mockMvc.perform(put("/products/{id}", existingId)
                        .content(jsonBody)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))

                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.name").exists())
                        .andExpect(jsonPath("$.description").exists());
    }

    @Test
    public void updateShouldReturnNotFoundWhenNonExistingId() throws Exception{
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        String accessToken = tokenUtil.obtainAccessToken(mockMvc, username, password);

        mockMvc.perform(put("/products/{id}", nonExistingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(jsonBody)
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isNotFound());
    }

    @Test
    public void findByIdShouldReturnNotFoundWhenNonExistingId() throws Exception{
        mockMvc.perform(get("/products/{id}", nonExistingId).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void findByIdShouldReturnProductWhenExistingId() throws Exception{
        mockMvc.perform(get("/products/{id}", existingId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    public void findAllShouldReturnPage() throws Exception{
        //Essa linha aqui de baixo faz a mesma coisa que o trecho abaixo, por??m numa linha s??
        //mockMvc.perform(get("/products").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

        ResultActions result =
                mockMvc.perform(get("/products")
                        .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
    }
}
