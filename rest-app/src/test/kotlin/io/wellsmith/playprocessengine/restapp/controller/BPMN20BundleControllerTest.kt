package io.wellsmith.playprocessengine.restapp.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringJUnitWebConfig
@SpringBootTest
@AutoConfigureMockMvc
class BPMN20BundleControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Test
  fun `can post and retrieve a zipped XML bundle`() {

    val filename = "two bpmn xml files.zip"
    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
    val multipartFile = MockMultipartFile("file", filename, "application/zip", inputStream)
    val postResult = mockMvc.perform(MockMvcRequestBuilders.multipart("/bpmn20/bundle")
        .file(multipartFile))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
        .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.LOCATION))
        .andReturn()

    val bundleLocation = postResult.response.getHeader(HttpHeaders.LOCATION)
    val bundleId = bundleLocation!!.substringAfterLast("/")
    val getBundleResult = mockMvc.perform(MockMvcRequestBuilders.get("/bpmn20/bundle/$bundleId"))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
        .andReturn()

    val ids = objectMapper.readValue(
        getBundleResult.response.contentAsString, Array<String>::class.java)
    Assertions.assertEquals(2, ids.size)

    val getXMLResult = mockMvc.perform(MockMvcRequestBuilders.get("/bpmn20/definitions/${ids[0]}"))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
        .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
  }
}