package root.cyb.mh.attendancesystem.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionVaultDto {
    private List<FolderDto> folders;
    private List<ResourceDto> resources;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderDto {
        private Long id;
        private String name;
        private Long parentId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceDto {
        private Long id;
        private String resourceName;
        private String resourceLink;
        private String loginId;
        private String password;
        private Long folderId;
    }
}
