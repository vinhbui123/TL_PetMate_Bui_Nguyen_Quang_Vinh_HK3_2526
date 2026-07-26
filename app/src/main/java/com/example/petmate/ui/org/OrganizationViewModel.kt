package com.example.petmate.ui.org

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petmate.model.OrgDocumentDto
import com.example.petmate.model.OrganizationProfileDto
import com.example.petmate.repository.OrganizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class OrganizationViewModel(private val repo: OrganizationRepository) : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep

    private val _formData = MutableStateFlow(OrganizationProfileDto())
    val formData: StateFlow<OrganizationProfileDto> = _formData

    private val _uploadedDocs = MutableStateFlow<List<OrgDocumentDto>>(emptyList())
    val uploadedDocs: StateFlow<List<OrgDocumentDto>> = _uploadedDocs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess

    val existingOrg = MutableStateFlow<OrganizationProfileDto?>(null)

    fun isIndependentFoster(): Boolean = _formData.value.orgType == "INDEPENDENT_FOSTER"

    fun resetSubmitSuccess() {
        _submitSuccess.value = false
    }

    fun resetState() {
        _submitSuccess.value = false
        _currentStep.value = 1
        _formData.value = OrganizationProfileDto()
        _uploadedDocs.value = emptyList()
        _error.value = null
        existingOrg.value = null
    }

    fun updateFormData(update: (OrganizationProfileDto) -> OrganizationProfileDto) {
        _formData.value = update(_formData.value)
    }

    fun nextStep() {
        if (validateStep(_currentStep.value)) {
            _currentStep.value = (_currentStep.value + 1).coerceAtMost(6)
            _error.value = null
        }
    }

    fun prevStep() {
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(1)
        _error.value = null
    }
    
    fun setStep(step: Int) {
        _currentStep.value = step.coerceIn(1, 6)
    }

    private fun validateStep(step: Int): Boolean {
        // Add specific validation logic here if needed. 
        // For now, allow navigation, we will validate on final submit or rely on UI states.
        return true
    }

    fun loadExistingOrg() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = repo.getMyOrg()
                if (resp.isSuccessful && resp.body() != null) {
                    val org = resp.body()!!
                    existingOrg.value = org
                    if (org.status == "NEEDS_SUPPLEMENT" || org.status == "PENDING") {
                        _formData.value = org
                        _uploadedDocs.value = org.documents
                    }
                } else {
                    existingOrg.value = null
                }
            } catch (e: Exception) {
                existingOrg.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadDocument(docType: String, fileBytes: ByteArray, fileName: String) {
        val orgId = existingOrg.value?.id ?: _formData.value.id
        if (orgId == null) {
            _error.value = "Organization ID missing. Please save the form first."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val reqBody = fileBytes.toRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, reqBody)
                val resp = repo.uploadDocument(orgId, docType, part)
                if (resp.isSuccessful) {
                    val newDoc = resp.body()
                    if (newDoc != null) {
                        _uploadedDocs.value = _uploadedDocs.value + newDoc
                    }
                } else {
                    _error.value = "Failed to upload document: ${resp.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun removeDocument(docId: Long) {
        val orgId = existingOrg.value?.id ?: _formData.value.id ?: return
        viewModelScope.launch {
            try {
                val resp = repo.deleteDocument(orgId, docId)
                if (resp.isSuccessful) {
                    _uploadedDocs.value = _uploadedDocs.value.filter { it.id != docId }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun submit() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val isUpdate = _formData.value.id != null
                val resp = if (isUpdate) {
                    repo.updateOrg(_formData.value.id!!, _formData.value)
                } else {
                    repo.registerOrg(_formData.value)
                }

                if (resp.isSuccessful) {
                    _submitSuccess.value = true
                    val updatedOrg = resp.body()
                    if (updatedOrg != null) {
                        _formData.value = updatedOrg
                        existingOrg.value = updatedOrg
                    }
                } else {
                    _error.value = parseErrorBody(resp)
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseErrorBody(response: retrofit2.Response<*>): String {
        return try {
            val errorBodyString = response.errorBody()?.string()
            if (!errorBodyString.isNullOrBlank()) {
                try {
                    val json = org.json.JSONObject(errorBodyString)
                    // Common Spring Boot error keys: message, error, reason
                    json.optString("message", json.optString("error", "Lỗi ${response.code()}"))
                } catch (e: org.json.JSONException) {
                    // Not a JSON, return the raw string if it's short, otherwise status code
                    if (errorBodyString.length < 200) errorBodyString else "Lỗi ${response.code()}: ${response.message()}"
                }
            } else {
                "Lỗi ${response.code()}: ${response.message()}"
            }
        } catch (e: Exception) {
            "Lỗi ${response.code()}: ${response.message()}"
        }
    }
    
    suspend fun updateOrgProfile(dto: OrganizationProfileDto): Boolean {
        _isLoading.value = true
        _error.value = null
        return try {
            val resp = repo.updateOrg(dto.id!!, dto)
            if (resp.isSuccessful) {
                existingOrg.value = resp.body()
                true
            } else {
                _error.value = parseErrorBody(resp)
                false
            }
        } catch (e: Exception) {
            _error.value = e.message
            false
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun updateOrgProfileWithLogo(
        id: Long,
        dto: OrganizationProfileDto,
        imageUri: android.net.Uri?,
        context: android.content.Context
    ): Boolean {
        _isLoading.value = true
        _error.value = null
        try {
            var updatedDto = dto
            if (imageUri != null) {
                // upload logo
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val tempFile = java.io.File.createTempFile("upload", ".jpg", context.cacheDir)
                val outputStream = java.io.FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                
                val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = okhttp3.MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                
                val uploadResp = repo.uploadLogo(id, body)
                if (uploadResp.isSuccessful) {
                    updatedDto = updatedDto.copy(logoUrl = uploadResp.body()?.logoUrl)
                } else {
                    _error.value = "Lỗi tải ảnh: ${uploadResp.code()} ${uploadResp.message()}"
                    return false
                }
            }
            
            // update rest of profile
            val resp = repo.updateOrg(id, updatedDto)
            if (resp.isSuccessful) {
                existingOrg.value = resp.body()
                return true
            } else {
                _error.value = parseErrorBody(resp)
                return false
            }
        } catch (e: Exception) {
            _error.value = e.message
            return false
        } finally {
            _isLoading.value = false
        }
    }
    
    fun getRequiredDocTypes(): List<String> {
        return if (isIndependentFoster()) {
            listOf("ID_CARD", "VET_COOPERATION", "LIVING_SPACE_PHOTO")
        } else {
            listOf("BUSINESS_LICENSE", "VET_COOPERATION", "FACILITY_PHOTO")
        }
    }

    suspend fun leaveOrganization(orgId: Long): Boolean {
        _isLoading.value = true
        _error.value = null
        return try {
            val resp = repo.leaveOrganization(orgId)
            if (resp.isSuccessful) {
                true
            } else {
                _error.value = parseErrorBody(resp)
                false
            }
        } catch (e: Exception) {
            _error.value = e.message
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun dissolveOrganization(orgId: Long): Boolean {
        _isLoading.value = true
        _error.value = null
        return try {
            val resp = repo.dissolveOrganization(orgId)
            if (resp.isSuccessful) {
                true
            } else {
                _error.value = parseErrorBody(resp)
                false
            }
        } catch (e: Exception) {
            _error.value = e.message
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun transferOwnership(orgId: Long, newOwnerId: Long): Boolean {
        _isLoading.value = true
        _error.value = null
        return try {
            val resp = repo.transferOwnership(orgId, newOwnerId)
            if (resp.isSuccessful) {
                true
            } else {
                _error.value = parseErrorBody(resp)
                false
            }
        } catch (e: Exception) {
            _error.value = e.message
            false
        } finally {
            _isLoading.value = false
        }
    }
}
