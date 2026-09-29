package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.model.Dogadjaj;
import ba.sum.fsre.zupniured.repository.DogadjajRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DogadjajService {

    private final DogadjajRepository dogadjajRepository;

    public DogadjajService(DogadjajRepository dogadjajRepository) {
        this.dogadjajRepository = dogadjajRepository;
    }

    public List<Dogadjaj> pronadjiSve() {
        return dogadjajRepository.findAll(Sort.by(Sort.Direction.ASC, "datum", "vrijeme"));
    }

    public Dogadjaj spremi(Dogadjaj dogadjaj) {
        return dogadjajRepository.save(dogadjaj);
    }
    
    public Dogadjaj pronadjiPoId(Long id) {
        return dogadjajRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Događaj nije pronađen."));
    }

    public void obrisi(Long id) {
        dogadjajRepository.deleteById(id);
    }
}
