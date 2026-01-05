package com.example.housebackend.service;

import com.example.housebackend.domain.location.Region;
import com.example.housebackend.domain.location.SubwayLine;
import com.example.housebackend.exception.ResourceNotFoundException;
import com.example.housebackend.repository.RegionRepository;
import com.example.housebackend.repository.SubwayLineRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final RegionRepository regionRepository;
    private final SubwayLineRepository subwayLineRepository;

    @Transactional
    public Region createRegion(Region region) {
        return regionRepository.save(region);
    }

    @Transactional
    public Region updateRegion(Long regionId, Region incoming) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("地区不存在"));
        region.setName(incoming.getName());
        region.setDescription(incoming.getDescription());
        return regionRepository.save(region);
    }

    @Transactional
    public void deleteRegion(Long regionId) {
        regionRepository.deleteById(regionId);
    }

    @Transactional(readOnly = true)
    public List<Region> listRegions() {
        return regionRepository.findAll();
    }

    @Transactional
    public SubwayLine createSubway(SubwayLine subwayLine) {
        return subwayLineRepository.save(subwayLine);
    }

    @Transactional
    public SubwayLine updateSubway(Long subwayId, SubwayLine incoming) {
        SubwayLine subwayLine = subwayLineRepository.findById(subwayId)
                .orElseThrow(() -> new ResourceNotFoundException("地铁信息不存在"));
        subwayLine.setLineName(incoming.getLineName());
        subwayLine.setStationName(incoming.getStationName());
        subwayLine.setRegion(incoming.getRegion());
        return subwayLineRepository.save(subwayLine);
    }

    @Transactional
    public void deleteSubway(Long subwayId) {
        subwayLineRepository.deleteById(subwayId);
    }

    @Transactional(readOnly = true)
    public List<SubwayLine> listSubway() {
        return subwayLineRepository.findAll();
    }
}
