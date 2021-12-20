package sg.edu.iss.LAPS.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.LAPS.model.LeaveApplied;
import sg.edu.iss.LAPS.model.OverseasLeaveDetails;
import sg.edu.iss.LAPS.repo.LeaveAppliedRepository;
import sg.edu.iss.LAPS.repo.OverseasLeaveRepository;
import sg.edu.iss.LAPS.utility.LeaveStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LeaveAppliedServiceImpl implements LeaveAppliedService {
    @Autowired
    LeaveAppliedRepository repository;

    @Autowired
    OverseasLeaveRepository overseasLeaveRepository;

    @Override
    public Optional<LeaveApplied> findById(int id) {
        return repository.findById(id);
    }

    @Override
    public List<LeaveApplied> findByUserId(Long userID) {
        return repository.findByUserId(userID);
    }

    @Override
    public List<LeaveApplied> findByUserId(Long userID, LeaveStatus status) {
        List<LeaveApplied> list = this.findByUserId(userID);
        return list.stream()
                .filter(item -> item.getApprovalStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void update(LeaveApplied leaveApplied) {
        OverseasLeaveDetails overseasLeaveDetails = this.overseasLeaveRepository.save(leaveApplied.getOverseasTrip());

        Optional<LeaveApplied> optLeaveApplied = this.findById(leaveApplied.getLeaveAppliedId());
        LeaveApplied savedLeaveApplied = leaveApplied;
        if (optLeaveApplied.isPresent()) {
            savedLeaveApplied = optLeaveApplied.get();
            savedLeaveApplied.setLeaveType(leaveApplied.getLeaveType());
            savedLeaveApplied.setLeaveStartDate(leaveApplied.getLeaveStartDate());
            savedLeaveApplied.setLeaveEndDate(leaveApplied.getLeaveEndDate());
            savedLeaveApplied.setIsOverseas(leaveApplied.getIsOverseas());
            savedLeaveApplied.setLeaveReason(leaveApplied.getLeaveReason());
            savedLeaveApplied.setWorkDissemination(leaveApplied.getWorkDissemination());
            savedLeaveApplied.setOverseasTrip(overseasLeaveDetails);
            // FIXME: count number of days
            savedLeaveApplied.setNoOfDays(0);
        }

        repository.save(savedLeaveApplied);
    }

    @Override
    public void delete(int id) {
        Optional<LeaveApplied> optional = this.findById(id);
        if (optional.isPresent()) {
            LeaveApplied leaveApplied = optional.get();
            leaveApplied.setApprovalStatus(LeaveStatus.DELETED);
            repository.save(leaveApplied);
        }
    }
}
