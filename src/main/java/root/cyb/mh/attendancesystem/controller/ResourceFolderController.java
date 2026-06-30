package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import root.cyb.mh.attendancesystem.model.Employee;
import root.cyb.mh.attendancesystem.model.ResourceFolder;
import root.cyb.mh.attendancesystem.model.SharedResource;
import root.cyb.mh.attendancesystem.repository.EmployeeRepository;
import root.cyb.mh.attendancesystem.repository.SharedResourceRepository;
import root.cyb.mh.attendancesystem.service.ResourceFolderService;

import java.util.List;

@Controller
@RequestMapping("/admin/resource-folders")
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public class ResourceFolderController {

    @Autowired
    private ResourceFolderService folderService;

    @Autowired
    private SharedResourceRepository sharedResourceRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping
    public String viewFolders(@RequestParam(required = false) Long folderId, Model model) {
        model.addAttribute("activeLink", "resource-folders");
        model.addAttribute("employees", employeeRepository.findAll());

        if (folderId == null) {
            model.addAttribute("currentFolder", null);
            model.addAttribute("currentFolders", folderService.getAllRootFolders());
            model.addAttribute("currentResources", java.util.Collections.emptyList());
            model.addAttribute("breadcrumbs", java.util.Collections.emptyList());
        } else {
            ResourceFolder currentFolder = folderService.getFolderById(folderId);
            model.addAttribute("currentFolder", currentFolder);
            model.addAttribute("currentFolders", currentFolder != null ? currentFolder.getSubFolders() : java.util.Collections.emptyList());
            model.addAttribute("currentResources", currentFolder != null ? currentFolder.getResources() : java.util.Collections.emptyList());
            model.addAttribute("assignedEmployees", currentFolder != null ? currentFolder.getAllowedEmployees() : java.util.Collections.emptySet());
            model.addAttribute("inheritedEmployees", currentFolder != null ? folderService.getInheritedEmployeesWithSourceFolder(currentFolder) : java.util.Collections.emptyMap());
            List<ResourceFolder> breadcrumbs = new java.util.ArrayList<>();
            ResourceFolder temp = currentFolder;
            while (temp != null) {
                breadcrumbs.add(0, temp);
                temp = temp.getParentFolder();
            }
            model.addAttribute("breadcrumbs", breadcrumbs);
        }
        return "group-resources";
    }

    @PostMapping("/create")
    public String createFolder(@RequestParam String name, 
                               @RequestParam(required = false) Long parentId, 
                               RedirectAttributes redirectAttributes) {
        try {
            folderService.createFolder(name, parentId);
            redirectAttributes.addFlashAttribute("successMessage", "Folder created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating folder: " + e.getMessage());
        }
        return "redirect:/admin/resource-folders" + (parentId != null ? "?folderId=" + parentId : "");
    }

    @PostMapping("/delete")
    public String deleteFolder(@RequestParam Long id, @RequestParam(required = false) Long parentId, RedirectAttributes redirectAttributes) {
        try {
            folderService.deleteFolder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Folder deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete folder. Ensure it is empty.");
        }
        return "redirect:/admin/resource-folders" + (parentId != null ? "?folderId=" + parentId : "");
    }

    @PostMapping("/{folderId}/resources/add")
    public String addResource(@PathVariable Long folderId, 
                              @ModelAttribute SharedResource resource, 
                              RedirectAttributes redirectAttributes) {
        try {
            ResourceFolder folder = folderService.getFolderById(folderId);
            if (folder != null) {
                resource.setFolder(folder);
                // Ensure employee is null so it's a folder resource
                resource.setEmployee(null); 
                sharedResourceRepository.save(resource);
                redirectAttributes.addFlashAttribute("successMessage", "Resource added to folder successfully.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding resource.");
        }
        return "redirect:/admin/resource-folders?folderId=" + folderId;
    }
    
    @PostMapping("/resources/delete")
    public String deleteResource(@RequestParam Long resourceId, @RequestParam Long folderId, RedirectAttributes redirectAttributes) {
        try {
            sharedResourceRepository.deleteById(resourceId);
            redirectAttributes.addFlashAttribute("successMessage", "Resource deleted.");
        } catch(Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting resource.");
        }
        return "redirect:/admin/resource-folders?folderId=" + folderId;
    }

    @PostMapping("/resources/edit")
    public String editResource(@ModelAttribute SharedResource updatedResource, 
                               @RequestParam Long resourceId, 
                               @RequestParam Long folderId, 
                               RedirectAttributes redirectAttributes) {
        try {
            SharedResource existing = sharedResourceRepository.findById(resourceId).orElse(null);
            if (existing != null) {
                existing.setResourceName(updatedResource.getResourceName());
                existing.setResourceLink(updatedResource.getResourceLink());
                existing.setLoginId(updatedResource.getLoginId());
                existing.setPassword(updatedResource.getPassword());
                sharedResourceRepository.save(existing);
                redirectAttributes.addFlashAttribute("successMessage", "Resource updated successfully.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating resource.");
        }
        return "redirect:/admin/resource-folders?folderId=" + folderId;
    }

    @PostMapping("/{folderId}/assign")
    public String assignEmployee(@PathVariable Long folderId, 
                                 @RequestParam String employeeId, 
                                 RedirectAttributes redirectAttributes) {
        try {
            Employee emp = employeeRepository.findById(employeeId).orElse(null);
            if (emp != null) {
                folderService.assignEmployeeToFolder(folderId, emp);
                redirectAttributes.addFlashAttribute("successMessage", "Employee assigned to folder.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error assigning employee.");
        }
        return "redirect:/admin/resource-folders?folderId=" + folderId;
    }

    @PostMapping("/{folderId}/remove-employee")
    public String removeEmployee(@PathVariable Long folderId, 
                                 @RequestParam String employeeId, 
                                 RedirectAttributes redirectAttributes) {
        try {
            Employee emp = employeeRepository.findById(employeeId).orElse(null);
            if (emp != null) {
                folderService.removeEmployeeFromFolder(folderId, emp);
                redirectAttributes.addFlashAttribute("successMessage", "Employee removed from folder.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error removing employee.");
        }
        return "redirect:/admin/resource-folders?folderId=" + folderId;
    }
}
