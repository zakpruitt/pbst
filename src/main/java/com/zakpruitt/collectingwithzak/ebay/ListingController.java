package com.zakpruitt.collectingwithzak.ebay;

import com.zakpruitt.jbay.JbayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    public String renderIndex(Model model) {
        model.addAttribute("data", listingService.getIndexData());
        model.addAttribute("page", "listings");
        return "listings/index";
    }

    @PostMapping("/review")
    public String renderReview(@RequestParam("selectedKeys") List<String> selectedKeys, Model model) {
        model.addAttribute("rows", listingService.buildReview(selectedKeys));
        model.addAttribute("page", "listings");
        return "listings/review";
    }

    @PostMapping("/stage")
    public String stage(StageListingsRequest request, RedirectAttributes redirectAttributes) {
        ListingService.StageResult result = listingService.stageListings(request);
        if (result.created() > 0) {
            redirectAttributes.addFlashAttribute("stagedCount", result.created());
        }
        if (!result.failures().isEmpty()) {
            redirectAttributes.addFlashAttribute("stageFailures", result.failures());
        }
        return "redirect:/listings";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            listingService.publish(id);
            redirectAttributes.addFlashAttribute("publishedId", id);
        } catch (JbayException e) {
            redirectAttributes.addFlashAttribute("stageFailures",
                    List.of("Publish failed: " + e.getMessage()));
        }
        return "redirect:/listings";
    }

    @PostMapping("/{id}/delete")
    public Object delete(@PathVariable Long id,
                         @RequestHeader(value = "HX-Request", required = false) String hx) {
        listingService.delete(id);
        if (hx != null) {
            return ResponseEntity.noContent().build();
        }
        return "redirect:/listings";
    }
}
