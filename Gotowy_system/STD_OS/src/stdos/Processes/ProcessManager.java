package stdos.Processes;

import java.util.ArrayList;
import java.util.List;
import stdos.CPU.*;
import stdos.VM.VirtualMemory;

public class ProcessManager {
    private static int actPid;
    private static List<PCB> activeProcesses;
    private static List<PCB> readyProcesses;
    private static String idleProcessFilename = "DUMMYFILE";

    //static CPU cpu = new CPU();

    /*
    Constructor of ProcessManager()
    Create lists of all and ready processes
    Create Idle Process (Static priority = 0)
     */
    public ProcessManager() {
        activeProcesses = new ArrayList<PCB>();
        readyProcesses = new ArrayList<PCB>();
        try {
            KM_CreateProcess(idleProcessFilename, "DUMMY", 0);
        } catch(Exception e) {
            System.out.println(e);
        }
        return;
    }

    /*
    KM_CreateProcess
    _filename = filename of file which have code of process
    _processname = processname of new process
    _p = static priority of process
    return PCB of new process
     */
    public static PCB KM_CreateProcess (String _filename, String _processname, int _p) throws Exception {
        if(_filename.trim().equals("")) { //Check if filename is not ""
            throw new Exception("KM_CreateProcess:fileNameMustNotBeNull");
        }
        if(_processname.trim().equals("")) { // Check if processname is not ""
            throw new Exception("KM_CreateProcess:processNameMustNotBeNull");
        }
        if(_filename.equals(idleProcessFilename)) { // Check if filename is not idle process filename
            if(_p!=0) throw new Exception("KM_CreateProcess:idleProcessPriorityMustBeZero");
        } else {
            if(_p==0) throw new Exception("KM_CreateProcess:notIdleProcessPriorityMustBeGreaterThanZero");
        }
        if(_p<1||_p>15) {
            if(!(_p==0&&_filename.equals(idleProcessFilename))) {
                throw new Exception("KM_CreateProcess:priorityOutsideRange");
            }
        }

        if(true) {//TODO: File exist?
            //TODO: czyPjest -> is file exist
            PCB pcb1 = new PCB(actPid, _filename, _processname, _p);
            actPid++;
            VirtualMemory.load_to_virtualmemory(actPid, ""); //TODO: program data
            KM_setProcessState(pcb1, ProcessState.READY);
            activeProcesses.add(pcb1);
            readyProcesses.add(pcb1);
            CPU.MM_addReadyProcess(pcb1);
            //TODO: line under is useless?
            if(_filename.equals(idleProcessFilename)) { pcb1.setPs(ProcessState.READY); readyProcesses.add(pcb1); }
        } else {
            throw new Exception("KM_CreateProcess:FileNotExist");
        }
        //TODO: check this
        return null;
    }

    public static void KM_CreateProcess (String _filename, int _p) throws Exception {
        ProcessManager.KM_CreateProcess(_filename, _filename, _p);
    }

    public static void KM_CreateProcess (String _filename, String _processname) throws Exception {
        ProcessManager.KM_CreateProcess(_filename, _processname, 1);
    }

    public static void KM_CreateProcess (String _filename) throws Exception {
        ProcessManager.KM_CreateProcess(_filename, _filename, 1);
    }

    public static boolean KM_TerminateProcess (PCB pcb) { //TODO: function
        if(pcb.getPid()==0) {
            return false;
        }
        activeProcesses.remove(pcb);
        readyProcesses.remove(pcb);
        CPU.MM_unreadyProcess(pcb);
        VirtualMemory.remove_from_virtualmemory(pcb.getPid());
        return true;
    }

    public static boolean KM_TerminateProcess (String _processname) {
        PCB pcb = KM_getPCBbyPN(_processname);
        if(pcb==null) {
            return false;
        } else {
            return KM_TerminateProcess(pcb);
        }
    }

    public static boolean KM_TerminateProcess (int pid) {
        PCB pcb = KM_getPCBbyPID(pid);
        if(pcb==null) {
            return false;
        } else {
            return KM_TerminateProcess(pcb);
        }
    }

    /*
    Allowed ProcessState changes
    NEW -> READY

    READY -> RUNNING

    WAITING -> READY

    RUNNING -> READY
    RUNNING -> WAITING

    Removed: READY -> WAITING

     */

    public static boolean KM_setProcessState (PCB _pcb, ProcessState _ps) {
        if(_ps==_pcb.getPs()) { //Before = after
            return true;
        } else if(_ps==ProcessState.NEW) { //Error
            return false;
        } else if(_ps==ProcessState.READY) {
            if(_pcb.getPs()==ProcessState.WAITING||_pcb.getPs()==ProcessState.NEW) {
                _pcb.setPs(_ps);
                readyProcesses.add(_pcb);
                CPU.MM_addReadyProcess(_pcb);
                return true;
            } else if(_pcb.getPs()==ProcessState.RUNNING) {
                _pcb.setPs(_ps);
                return true;
            }
        } else if(_ps==ProcessState.RUNNING) {
            if(_pcb.getPs()==ProcessState.READY) {
                _pcb.setPs(_ps);
                return true;
            }
        } else if(_ps==ProcessState.WAITING) {
            if(_pcb.getPs()==ProcessState.RUNNING) {
                readyProcesses.remove(_pcb);
                CPU.MM_unreadyProcess(_pcb);
                _pcb.setPs(_ps);
                return true;
            }
        }
        return false;
    }

    /*
    KM_setProcessDynamicPriority - set dynamic priority of process, by given PCB (_pcb) and priority (p)
     */
    public static boolean KM_setProcessDynamicPriority (PCB _pcb, int p) {
        if(p>=1&&p<=15) {
            _pcb.setPriD(p);
            return true;
        } else {
            return false;
        }
    }

    /*
    KM_setProcessDynamicPriorityDefault - set dynamic priority of process, by given PCB (_pcb) and priority (p)
    */
    public static boolean KM_setProcessDynamicPriorityDefault (PCB _pcb) {
        _pcb.setPriD(_pcb.getPriS());
        return true;
    }

    public static void KM_getAllProcessListPrint() {
        System.out.print("All Process List:\n");
        System.out.format("%-3s %-16s %-16s %-2s %-2s %-7s \n", "PID", "ProName", "FileName", "PS", "PD", "PState");
        for(PCB _p  : activeProcesses) {
            System.out.format("%-3d %-16s %-16s %-2d %-2d %-7s\n", _p.getPid(), _p.getPn(), _p.getFilename(), _p.getPriS(), _p.getPriD(), _p.getPs());
            //System.out.print("- "+_p.getPid()+"\t"+_p.getPn()+"\t"+_p.getFilename()+"\t\t"+_p.getPriS()+"\t\t"+_p.getPriD()+"\t\t"+_p.getPs()+"\n");
        }
        return;
    }

    public static void KM_getReadyProcessListPrint() {
        System.out.print("Ready Process List:\n");
        System.out.format("%-3s %-16s %-16s %-2s %-2s\n", "PID", "ProName", "FileName", "PS", "PD");
        for(PCB _p  : activeProcesses) {
            if(_p.getPs()==ProcessState.READY) {
                System.out.format("%-3d %-16s %-16s %-2d %-2d\n", _p.getPid(), _p.getPn(), _p.getFilename(), _p.getPriS(), _p.getPriD());
            }
        }
        return;
    }

    public static List<PCB> KM_getAllProcessList() {
        return activeProcesses;
    }

    public static List<PCB> KM_getReadyProcessList() {
        //return readyProcesses; //too simple
        if(activeProcesses.isEmpty()) {
            return null;
        }
        List<PCB> readyProc = new ArrayList<PCB>();
        for (PCB _p : activeProcesses) {
            if(_p.getPs()==ProcessState.READY) {
                readyProc.add(_p);
            }
        }
        return readyProc;
    }

    public static void KM_printRunningRegisters() { //For Interface
        PCB pcb = CPU.RUNNING;
        System.out.printf("Register state for process PID: %d, ProcessName: %s, Filename: %s\n", pcb.getPid(), pcb.getPn(), pcb.getFilename());
        System.out.printf("AX: %d, BX: %d, CX: %d, DX: %d\n", pcb.getAx(), pcb.getBx(), pcb.getCx(), pcb.getDx());
    }

    public static PCB KM_getPCBbyPID (int pid) {
        for(PCB _p : activeProcesses) {
            if(_p.getPid()==pid) {
                return _p;
            }
        }
        return null;
    }

    public static PCB KM_getPCBbyPN (String _processname) {
        for(PCB _p : activeProcesses) {
            if(_p.getPn().equalsIgnoreCase(_processname)) {
                return _p;
            }
        }
        return null;
    }
}
