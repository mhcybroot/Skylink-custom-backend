package root.cyb.mh.attendancesystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.ResourceFolder;
import root.cyb.mh.attendancesystem.repository.ResourceFolderRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ResourceFolderService {

    @Autowired
    private ResourceFolderRepository resourceFolderRepository;

    public List<ResourceFolder> getAllRootFolders() {
        return resourceFolderRepository.findByParentFolderIsNull();
    }

    public ResourceFolder getFolderById(Long id) {
        return resourceFolderRepository.findById(id).orElse(null);
    }

    @Transactional
    public ResourceFolder createFolder(String name, Long parentId) {
        ResourceFolder folder = new ResourceFolder();
        folder.setName(name);
        if (parentId != null) {
            ResourceFolder parent = getFolderById(parentId);
            folder.setParentFolder(parent);
        }
        return resourceFolderRepository.save(folder);
    }

    @Transactional
    public void deleteFolder(Long id) {
        resourceFolderRepository.deleteById(id);
    }

    @Transactional
    public void assignEmployeeToFolder(Long folderId, Employee employee) {
        ResourceFolder folder = getFolderById(folderId);
        if (folder != null) {
            folder.getAllowedEmployees().add(employee);
            resourceFolderRepository.save(folder);
        }
    }

    @Transactional
    public void removeEmployeeFromFolder(Long folderId, Employee employee) {
        ResourceFolder folder = getFolderById(folderId);
        if (folder != null) {
            folder.getAllowedEmployees().remove(employee);
            resourceFolderRepository.save(folder);
        }
    }

    @Transactional(readOnly = true)
    public List<Long> getAllAccessibleFolderIdsForEmployee(Employee employee) {
        List<ResourceFolder> directlyAssigned = resourceFolderRepository.findByAllowedEmployeesContaining(employee);
        Set<Long> accessibleFolderIds = new HashSet<>();
        for (ResourceFolder folder : directlyAssigned) {
            collectFolderAndSubfolders(folder, accessibleFolderIds);
        }
        return new ArrayList<>(accessibleFolderIds);
    }

    private void collectFolderAndSubfolders(ResourceFolder folder, Set<Long> folderIds) {
        if (folderIds.contains(folder.getId())) return;
        folderIds.add(folder.getId());
        for (ResourceFolder sub : folder.getSubFolders()) {
            collectFolderAndSubfolders(sub, folderIds);
        }
    }

    @Transactional(readOnly = true)
    public java.util.Map<Employee, ResourceFolder> getInheritedEmployeesWithSourceFolder(ResourceFolder folder) {
        java.util.Map<Employee, ResourceFolder> inherited = new java.util.HashMap<>();
        ResourceFolder current = folder.getParentFolder();
        while (current != null) {
            for (Employee emp : current.getAllowedEmployees()) {
                if (!inherited.containsKey(emp)) {
                    inherited.put(emp, current);
                }
            }
            current = current.getParentFolder();
        }
        return inherited;
    }
}
