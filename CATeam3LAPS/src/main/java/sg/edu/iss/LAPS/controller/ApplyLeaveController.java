package sg.edu.iss.LAPS.controller;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import sg.edu.iss.LAPS.model.*;
import sg.edu.iss.LAPS.services.*;
import sg.edu.iss.LAPS.utility.DateTools;
import sg.edu.iss.LAPS.utility.LeaveDetails;
import sg.edu.iss.LAPS.validators.LeaveAppliedValidator;
import sg.edu.iss.LAPS.validators.OverseasLeaveDetailValidator;

import static sg.edu.iss.LAPS.utility.LeaveStatus.APPLIED;
import static sg.edu.iss.LAPS.utility.LeaveStatus.APPROVED;

@Controller
public class ApplyLeaveController {
    @Autowired
    private LeaveAppliedService leaveAppliedService;

    @Autowired
    private ApplyLeaveService applyLeaveService;

    @Autowired
    private UserService userService;

    @Autowired
    private LeaveTypeService leaveTypeService;

    @Autowired
    private PublicHolidayService publicHolidayService;
    
    @Autowired
    private OverseasLeaveService overseasLeaveService;
    
    @Autowired
    private LeaveAppliedValidator leaveAppliedValidator;
    
    @Autowired
    private OverseasLeaveDetailValidator overseasLeaveDetailsValidator;
    
    @Autowired
    private EmailNotificationService emailservice;

    @Autowired
    private LeaveEntitledService leaveEntitledService;
    
    
    @InitBinder("leaveapplication")
	protected void initBinder(WebDataBinder binder) {
		binder.addValidators(leaveAppliedValidator);
	}
    
//    @InitBinder("OverseasLeaveDetails")
//	protected void initBinder1(WebDataBinder binder) {
//		binder.addValidators(overseasLeaveDetailsValidator);
//	}
    

    @RequestMapping(value="/staff/applyleave")
	public String applyForm(Model model, HttpSession session) {
		model.addAttribute("leaveapplication", new LeaveApplied());
		List<LeaveType> leaveTypeList = leaveTypeService.getAllLeaveType();
		model.addAttribute("leaveTypeList", leaveTypeList);
		model.addAttribute("overseasTrip", new OverseasLeaveDetails());

        // Code for balances
        ArrayList<LeaveDetails> leaveDetails = new ArrayList<LeaveDetails>();
        for(LeaveType leaveType: leaveTypeList){
            LeaveDetails leaveDetail = new LeaveDetails();
            leaveDetail.setName(leaveType.getDescription());
            leaveDetail.setPending(leaveAppliedService.CalLeavesByStatus((Long)session.getAttribute("id"),leaveType.getLeaveTypeId(), APPLIED));
            leaveDetail.setTaken(leaveAppliedService.CalLeavesByStatus((Long)session.getAttribute("id"),leaveType.getLeaveTypeId(), APPROVED));
            leaveDetail.setAvailable(leaveEntitledService.totalAvailableLeave((Long)session.getAttribute("id"),leaveType.getLeaveTypeId()));
            leaveDetails.add(leaveDetail);
        }
        model.addAttribute("leaveDetails",leaveDetails);
		return "applyLeave";
	}


    @RequestMapping(value = "/staff/applyleave/submit")
    public String createLeaveApplication(@ModelAttribute("leaveapplication") @Valid LeaveApplied application, BindingResult bindingResult, HttpSession session, Model model) {
        if (bindingResult.hasErrors()) {
    		
    		List<LeaveType> leaveTypeList = leaveTypeService.getAllLeaveType();
    		model.addAttribute("leaveTypeList", leaveTypeList);
            // Code for balances
            ArrayList<LeaveDetails> leaveDetails = new ArrayList<LeaveDetails>();
            for(LeaveType lType: leaveTypeList){
                LeaveDetails leaveDetail = new LeaveDetails();
                leaveDetail.setName(lType.getDescription());
                leaveDetail.setPending(leaveAppliedService.CalLeavesByStatus((Long)session.getAttribute("id"),lType.getLeaveTypeId(), APPLIED));
                leaveDetail.setTaken(leaveAppliedService.CalLeavesByStatus((Long)session.getAttribute("id"),lType.getLeaveTypeId(), APPROVED));
                leaveDetail.setAvailable(leaveEntitledService.totalAvailableLeave((Long)session.getAttribute("id"),lType.getLeaveTypeId()));
                leaveDetails.add(leaveDetail);
            }
            model.addAttribute("leaveDetails",leaveDetails);

			return "applyLeave";
		}//appliedDate,LeaveType,@notnullapplieddate 
        
        User currUser = userService.findUserById((Long) session.getAttribute("id"));
        LeaveType leaveType = leaveTypeService.getLeaveTypeById(application.getLeaveType().getLeaveTypeId());
        application.setUser(currUser);
        application.setLeaveType(leaveType);
        Calendar appliedStartDate = DateTools.dateToCalendar(application.getLeaveStartDate());
        Calendar appliedEndDate = DateTools.dateToCalendar(application.getLeaveEndDate());
        float daysPeriod = ChronoUnit.DAYS.between(appliedStartDate.toInstant(), appliedEndDate.toInstant()) + 1;

        // Get a list of public holidays
        List<PublicHoliday> publicHolidaysList = publicHolidayService.findAll();
        List<PublicHoliday> holidaysAffectLeave1 = new ArrayList<>();
        List<PublicHoliday> holidaysAffectLeave2 = new ArrayList<>();
        List<PublicHoliday> holidaysAffectLeave3 = new ArrayList<>();
        // use for each to traverse the public holiday collection

        // TODO:clean code
        float WeekdaysPublicHoliday1 = 0;
        for (PublicHoliday holiday : publicHolidaysList) {
            Calendar calPublicHolidayStart = DateTools.dateToCalendar(holiday.getHolidayStartDate());
            Calendar calPublicHolidayEnd = DateTools.dateToCalendar(holiday.getHolidayEndDate());

            // find the public holidays from the start day to the 14th days
            if (holiday.getHolidayStartDate()
                    .after(application.getLeaveStartDate()) && holiday.getHolidayEndDate()
                    .before(application.getLeaveEndDate())) {
                holidaysAffectLeave1.add(holiday);
            }
        }

        float WeekdaysPublicHoliday2 = 0;
        for (PublicHoliday holiday : publicHolidaysList) {
            if (holiday.getHolidayStartDate()
                    .before(application.getLeaveStartDate()) && holiday.getHolidayEndDate()
                    .after(application.getLeaveStartDate())) {
                holidaysAffectLeave2.add(holiday);
            }
        }

        float WeekdaysPublicHoliday3 = 0;
        for (PublicHoliday holiday : publicHolidaysList) {
            if (holiday.getHolidayStartDate()
                    .before(application.getLeaveEndDate()) && holiday.getHolidayEndDate()
                    .after(application.getLeaveEndDate())) {
                holidaysAffectLeave3.add(holiday);
            }
        }

        // compute situation 1:
        for (PublicHoliday day : holidaysAffectLeave1) {
            WeekdaysPublicHoliday1 = DateTools.countWeekdaysPublicHoliday(DateTools.dateToCalendar(day.getHolidayStartDate()), DateTools.dateToCalendar(day.getHolidayEndDate())) - 1;
        }

        // compute situation 2:
		/*
		 * for (PublicHoliday day : holidaysAffectLeave2) { WeekdaysPublicHoliday2 =
		 * ChronoUnit.DAYS.between(appliedStartDate.toInstant(), day.getHolidayEndDate()
		 * .toInstant()) + 1; }
		 */


        // compute situation 3:
		/*
		 * for (PublicHoliday day : holidaysAffectLeave3) { WeekdaysPublicHoliday3 =
		 * ChronoUnit.DAYS.between(day.getHolidayStartDate() .toInstant(),
		 * appliedEndDate.toInstant()) + 1; }
		 */

        if (daysPeriod <= 14) {
            daysPeriod = DateTools.removeWeekends(appliedStartDate, appliedEndDate);
        }

        // minus the public holiday in weekdays.
        daysPeriod = daysPeriod - (WeekdaysPublicHoliday1 + WeekdaysPublicHoliday2 + WeekdaysPublicHoliday3);
        application.setNoOfDays(daysPeriod);
        application.setApprovalStatus(APPLIED);
        
//        if(application.getIsOverseas()) {
//        	
//        	OverseasLeaveDetails overseaTrip = overseasLeaveService.findOverseasLeaveDetailsByoverseasLeaveId(application.getOverseasTrip().getOverseasLeaveId());
//        	application.setOverseasTrip(overseaTrip);
//        }

        //Subtract from total leave
//        LeaveEntitled leaveEntitled = leaveEntitledService.findLeaveEntitledByUserAndLeaveId(currUser.getId(), leaveType.getLeaveTypeId());
//        leaveEntitled.setTotalLeave(leaveEntitled.getTotalLeave()-daysPeriod);
//        leaveEntitledService.saveLeaveEntitled(leaveEntitled);

        Float totalLeaveAvailable = leaveEntitledService.totalAvailableLeave((Long)session.getAttribute("id"),leaveType.getLeaveTypeId());

        if(totalLeaveAvailable<daysPeriod){
            model.addAttribute("error_message","You have exceeded the number of available leaves");
            List<LeaveType> leaveTypeList = leaveTypeService.getAllLeaveType();
            model.addAttribute("leaveTypeList", leaveTypeList);

            // Code for balances
            ArrayList<LeaveDetails> leaveDetails = new ArrayList<LeaveDetails>();
            for(LeaveType lType: leaveTypeList){
                LeaveDetails leaveDetail = new LeaveDetails();
                leaveDetail.setName(lType.getDescription());
                leaveDetail.setPending(leaveAppliedService.CalLeavesByStatus((Long)session.getAttribute("id"),lType.getLeaveTypeId(), APPLIED));
                leaveDetail.setTaken(leaveAppliedService.CalLeavesByStatus((Long)session.getAttribute("id"),lType.getLeaveTypeId(), APPROVED));
                leaveDetail.setAvailable(leaveEntitledService.totalAvailableLeave((Long)session.getAttribute("id"),lType.getLeaveTypeId()));
                leaveDetails.add(leaveDetail);
            }
            model.addAttribute("leaveDetails",leaveDetails);

            return "applyLeave";
        }
        else {
            applyLeaveService.createLeaveApplication(application);

            Long managerId = (long) currUser.getReportsTo(); //get manager Id
            User manager = userService.findUserById(managerId); //get manager user object
            emailservice.sendLeaveCreationSucessful(currUser, application); //staff notif send to himself that own application created
            emailservice.sendLeaveCreationtoManager(currUser, application, manager); //staff notif send to manager, prompt for approval
            

            return "redirect:/staff/viewHistory";
        }
        
        //localhost:8080/staff/applyleave		-->applyleave.html -->a href pressed
        //localhost:8080/staff/applyleave/submit-->
        //localhost:8080/staff/viewhistory
        
    }

}
