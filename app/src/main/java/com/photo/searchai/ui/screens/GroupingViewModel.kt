package com.photo.searchai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.searchai.core.data.repository.GroupRepository
import com.photo.searchai.core.database.entity.GroupEntity
import com.photo.searchai.core.database.entity.ImageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class GroupingViewModel @Inject constructor(private val groupRepository: GroupRepository) :
        ViewModel() {

    data class GroupUiModel(val group: GroupEntity, val previews: List<ImageEntity>)

    val groups: StateFlow<List<GroupUiModel>> =
            groupRepository
                    .getTopGroups()
                    .map { groups ->
                        groups.map { group ->
                            GroupUiModel(
                                    group = group,
                                    previews = groupRepository.getGroupPreviewImages(group.groupId)
                            )
                        }
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = emptyList()
                    )
}
